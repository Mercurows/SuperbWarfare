package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.player.PlayerVariable.Companion.modify
import com.atsuishio.superbwarfare.capability.sync.CapabilitySync
import com.atsuishio.superbwarfare.capability.sync.SyncTarget
import com.atsuishio.superbwarfare.capability.sync.SyncedCapability
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable
import java.util.*
import java.util.function.Consumer

@AutoRegisterCapability
class PlayerVariable : INBTSerializable<CompoundTag>, SyncedCapability {
    private var old: PlayerVariable? = null

    @JvmField
    var ammo: MutableMap<Ammo, Int> = EnumMap(Ammo::class.java)
    var activeThermalImaging: Boolean = false

    /** 玩家变量只对本人有意义 */
    override val syncTarget: SyncTarget
        get() = SyncTarget.SELF

    /**
     * 全量同步：先写热成像状态，再按 [Ammo] 的枚举顺序写各弹种的存量。
     * 前后顺序必须与 [readSync] 一致。
     */
    override fun writeSync(encoder: ByteBufEncoder, full: Boolean) {
        encoder.encodeBoolean(activeThermalImaging)
        encoder.encodeInt(Ammo.entries.size)

        for (type in Ammo.entries) {
            encoder.encodeInt(type.get(this))
        }
    }

    override fun readSync(decoder: ByteBufDecoder, full: Boolean) {
        activeThermalImaging = decoder.decodeBoolean()

        val size = decoder.decodeInt()
        for (index in 0 until size) {
            val count = decoder.decodeInt()

            // 弹种数量以本地枚举为准，多余的数据直接丢弃，避免越界
            if (index >= Ammo.entries.size) continue

            // 直接写入 map：这里应用的是服务端的权威数据，不能再拿客户端本地的
            // SERVER 配置上限去校验（Ammo#set 会因上限不符而静默丢弃）
            ammo[Ammo.entries[index]] = count
        }
    }

    /** 记录当前状态，配合 [equals] 判断 [modify] 是否真的产生了变化 */
    fun watch(): PlayerVariable {
        this.old = this.copy()
        return this
    }

    /**
     * 是否有未同步的变化。仅在 [watch] 之后调用有意义。
     */
    fun changed(): Boolean = old != null && old != this

    fun writeToNBT(): CompoundTag {
        val nbt = CompoundTag()

        for (type in Ammo.entries) {
            type.set(nbt, type.get(this))
        }

        nbt.putBoolean("ActiveThermalImaging", activeThermalImaging)

        return nbt
    }

    fun readFromNBT(tag: CompoundTag) {
        for (type in Ammo.entries) {
            type.set(this, type.get(tag))
        }

        activeThermalImaging = tag.getBoolean("ActiveThermalImaging")
    }

    fun copy(): PlayerVariable {
        val clone = PlayerVariable()

        for (type in Ammo.entries) {
            type.set(clone, type.get(this))
        }

        clone.activeThermalImaging = this.activeThermalImaging

        return clone
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerVariable) return false

        for (type in Ammo.entries) {
            if (type.get(this) != type.get(other)) return false
        }

        return activeThermalImaging == other.activeThermalImaging
    }

    override fun serializeNBT(): CompoundTag {
        return writeToNBT()
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        readFromNBT(nbt)
    }

    companion object {
        var ID: ResourceLocation = loc("player_variables")

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerVariable {
            return entity.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(PlayerVariable())
        }

        /**
         * 标记玩家变量已变化，由 [CapabilitySync] 在本 tick 结束时自动同步给该玩家。
         */
        @JvmStatic
        fun markDirty(entity: Entity) {
            CapabilitySync.markDirty(entity, ID)
        }

        /**
         * 编辑并自动同步玩家变量
         *
         * 数据没有实际变化时不会发包。
         */
        @JvmStatic
        fun modify(entity: Entity, consumer: Consumer<PlayerVariable>) {
            if (entity.level().isClientSide) return
            val cap = entity.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(PlayerVariable())

            cap.watch()
            consumer.accept(cap)

            if (!cap.changed()) return

            markDirty(entity)
        }
    }

    override fun hashCode(): Int {
        var result = activeThermalImaging.hashCode()
        result = 31 * result + (old?.hashCode() ?: 0)
        result = 31 * result + ammo.hashCode()
        return result
    }
}
