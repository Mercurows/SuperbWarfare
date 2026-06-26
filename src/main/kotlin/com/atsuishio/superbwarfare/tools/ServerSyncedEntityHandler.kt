package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.cleanAll
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.getEntries
import com.atsuishio.superbwarfare.tools.ServerSyncedEntityHandler.register
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.ConcurrentHashMap

/**
 * 服务端同步实体处理器 —— ClientSyncedEntityHandler 的服务端镜像。
 *
 * 载具和导弹每 tick 主动调用 [register] 将自身加入此列表。
 * 雷达/IFF 等消费者调用 [getEntries] 从此列表查询，避免遍历 level.allEntities。
 * 定期调用 [cleanAll] 清理已消失实体的过期条目。
 */
object ServerSyncedEntityHandler {

    data class Entry(
        val entityId: Int,
        val pos: Vec3,
        val eyePos: Vec3,
        val yRot: Float,
        val entityType: ResourceLocation,
        val nbt: CompoundTag,
        val nbtTick: Int,
        val targetPos: Vec3?,
        /** 隐身减益系数，非载具实体为 1.0 */
        val trackDistanceMultiply: Double,
        /** 实体离地高度 */
        val heightAboveGround: Double,
    )

    // dim string → entityId → Entry
    private val entities = ConcurrentHashMap<String, ConcurrentHashMap<Int, Entry>>()

    /**
     * 注册或更新实体。每 tick 由 VehicleEntity / MissileProjectile 调用。
     * NBT 仅在与上次同步间隔 >= SYNC_ENTITY_INTERVAL 时重新序列化。
     */
    fun register(entity: Entity, targetPos: Vec3? = null) {
        val level = entity.level()
        if (level.isClientSide) return
        val server = level.server ?: return
        if (entity is VehicleEntity && entity.isWreck) return
        if (entity !is VehicleEntity && entity !is MissileProjectile && !VehicleConfig.inScanList(entity.type)) return

        val dim = level.dimension().location().toString()
        val currentTick = server.tickCount
        val existing = entities[dim]?.get(entity.id)

        val interval = MiscConfig.SYNC_ENTITY_INTERVAL.get()
        val (nbt, nbtTick) = if (existing == null || currentTick - existing.nbtTick >= interval) {
            entity.serializeNBT() to currentTick
        } else {
            existing.nbt to existing.nbtTick
        }

        val td = if (entity is VehicleEntity && !entity.isWreck)
            entity.computed().trackDistanceMultiply else 1.0
        val hag = computeHeightAboveGround(entity)

        val entry = Entry(
            entityId = entity.id,
            pos = entity.position(),
            eyePos = entity.eyePosition,
            yRot = entity.yRot,
            entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.type) ?: return,
            nbt = nbt,
            nbtTick = nbtTick,
            targetPos = targetPos,
            trackDistanceMultiply = td,
            heightAboveGround = hag,
        )

        entities.getOrPut(dim) { ConcurrentHashMap() }[entity.id] = entry
    }

    fun unregister(entity: Entity) {
        if (entity.level().isClientSide) return
        entities[entity.level().dimension().location().toString()]?.remove(entity.id)
    }

    fun getEntries(dim: ResourceLocation): Collection<Entry> {
        return entities[dim.toString()]?.values ?: emptyList()
    }

    /** 计算实体离地高度（使用高度图，高效） */
    private fun computeHeightAboveGround(entity: Entity): Double {
        val level = entity.level()
        val blockPos = entity.onPos
        val surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, blockPos.x, blockPos.z)
        return (entity.y - surfaceY).coerceAtLeast(0.0)
    }

    /** 判定实体是否在地下（实体顶部低于地表） */
    fun isUnderground(entity: Entity): Boolean {
        val level = entity.level()
        val blockPos = entity.onPos
        val surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, blockPos.x, blockPos.z)
        return entity.y + entity.bbHeight < surfaceY
    }

    /**
     * 清理已消失实体的过期条目。每 200 tick 由 Mod.kt 调用。
     */
    fun cleanAll(server: MinecraftServer, staleTicks: Int = 600) {
        val currentTick = server.tickCount
        for (dimLevel in server.allLevels) {
            val dimKey = dimLevel.dimension().location().toString()
            val dimEntries = entities[dimKey] ?: continue
            dimEntries.values.removeIf { entry ->
                dimLevel.getEntity(entry.entityId) == null &&
                        currentTick - entry.nbtTick > staleTicks
            }
        }
    }
}
