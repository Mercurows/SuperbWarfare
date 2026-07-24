package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.annotation.ExcludeBvrSync
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player;
import java.util.concurrent.ConcurrentHashMap

/**
 * 超视距同步字段排除注册表。
 *
 * 在实体类加载时收集被 [ExcludeBvrSync] 标记的 NBT key，
 * 在构建超视距同步 NBT 时移除这些 key，以节省网络带宽。
 *
 * 线程安全，可被多个实体类同时注册。
 */
object BvrSyncExclusion {

    /** 实体类 → 需从超视距同步 NBT 中移除的 key 集合 */
    private val exclusions: MutableMap<Class<out Entity>, MutableSet<String>> = ConcurrentHashMap()

    /**
     * 获取指定实体类及其所有父类注册的排除 key 集合。
     * 向上遍历类继承链，合并所有层级的排除列表。
     */
    fun getExcludedKeys(entityClass: Class<out Entity>): Set<String> {
        var current: Class<*> = entityClass
        val allKeys = mutableSetOf<String>()
        while (Entity::class.java.isAssignableFrom(current)) {
            exclusions[current]?.let { allKeys.addAll(it) }
            current = current.superclass
        }
        return allKeys
    }

    /**
     * 扫描实体类的静态字段，收集被 [ExcludeBvrSync] 注解标记的 NBT key。
     * 使用 Java 反射，应在 companion object 的 init 块中调用一次。
     *
     * 仅处理类型为 [EntityDataAccessor] 的字段。
     */
    fun scanClass(entityClass: Class<out Entity>) {
        val keys = mutableSetOf<String>()
        for (field in entityClass.declaredFields) {
            if (!EntityDataAccessor::class.java.isAssignableFrom(field.type)) continue
            val annotation = field.getAnnotation(ExcludeBvrSync::class.java) ?: continue
            if (annotation.nbtKey.isNotEmpty()) {
                keys.add(annotation.nbtKey)
            }
        }
        if (keys.isNotEmpty()) {
            exclusions.getOrPut(entityClass) { ConcurrentHashMap.newKeySet() }.addAll(keys)
        }
    }

    /**
     * 手动注册需要排除的 NBT key。
     * 用于以下场景：
     * - NBT key 没有对应的 EntityDataAccessor（如 "Inventory"）
     * - 单个 EntityDataAccessor 对应多个 NBT key（如 Loiter 的 X/Y/Z/R）
     */
    fun registerDirectKeys(entityClass: Class<out Entity>, vararg keys: String) {
        exclusions.getOrPut(entityClass) { ConcurrentHashMap.newKeySet() }.addAll(keys)
    }

    /**
     * 从 CompoundTag 中移除所有已注册的排除 key。
     * 原地修改 tag，同时返回同一个实例以便链式调用。
     */
    fun stripExcludedKeys(tag: CompoundTag, entityClass: Class<out Entity>): CompoundTag {
        val excludedKeys = getExcludedKeys(entityClass)
        for (key in excludedKeys) {
            tag.remove(key)
        }
        return tag
    }
}

/**
 * Serializes an entity into a lightweight [CompoundTag] for Beyond Visual Range synchronization.
 *
 * @return lightweight [CompoundTag] for network transmission.
 */
fun Entity.getBvrSyncNbt(): CompoundTag {
    val tag = CompoundTag()

    // Fast-Path: Custom BVR syncable entity (Vehicles, Missiles, etc.)
    if (this is IBvrSyncableEntity) {
        this.buildBvrSyncNbt(tag)
        return tag
    }

    // Fast-Path: Player entities (bypasses massive Player profile / inventory / advancement NBT)
    if (this is Player) {
        val encodeId = this.encodeId ?: return tag
        tag.putString("id", encodeId)
        tag.putInt("EntityId", this.id)
        tag.putDouble("PosX", x)
        tag.putDouble("PosY", y)
        tag.putDouble("PosZ", z)
        tag.putFloat("Yaw", yRot)
        tag.putFloat("Pitch", xRot)
        tag.putUUID("UUID", this.uuid)
        return tag
    }

    // Legacy Fallback Path: Standard serialization with key stripping
    val fullTag = serializeNBT()
    BvrSyncExclusion.stripExcludedKeys(fullTag, javaClass)
    return fullTag
}