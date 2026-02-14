package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable
import java.util.*
import java.util.function.Consumer

@AutoRegisterCapability
class PlayerVariable : INBTSerializable<CompoundTag> {
    private var old: PlayerVariable? = null

    @JvmField
    var ammo: MutableMap<Ammo, Int> = EnumMap(Ammo::class.java)
    var activeThermalImaging: Boolean = false

    fun sync(entity: Entity) {
        if (!entity.getCapability(ModCapabilities.PLAYER_VARIABLE).isPresent) return

        val newVariable: PlayerVariable = getOrDefault(entity)
        if (old != null && old == newVariable) return

        if (entity is ServerPlayer) {
            sendPacketTo(entity, PlayerVariablesSyncMessage(entity.id, compareAndUpdate()))
        }
    }

    fun watch(): PlayerVariable {
        this.old = this.copy()
        return this
    }

    fun forceUpdate(): MutableMap<Byte, Int> {
        val map = HashMap<Byte, Int>()

        for (type in Ammo.entries) {
            map[type.ordinal.toByte()] = type.get(this)
        }

        map[(-1).toByte()] = if (this.activeThermalImaging) 1 else 0

        return map
    }

    fun compareAndUpdate(): MutableMap<Byte, Int> {
        val map = HashMap<Byte, Int>()
        val old = (if (this.old == null) PlayerVariable() else this.old)!!

        for (type in Ammo.entries) {
            val oldCount = old.ammo.getOrDefault(type, 0)
            val newCount = type.get(this)

            if (oldCount != newCount) {
                map[type.ordinal.toByte()] = newCount
            }
        }

        if (old.activeThermalImaging != this.activeThermalImaging) {
            map[(-1).toByte()] = if (this.activeThermalImaging) 1 else 0
        }

        return map
    }

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
         * 编辑并自动同步玩家变量
         */
        @JvmStatic
        fun modify(entity: Entity, consumer: Consumer<PlayerVariable>) {
            if (entity.level().isClientSide) return
            val cap = entity.getCapability(ModCapabilities.PLAYER_VARIABLE).orElse(PlayerVariable())

            cap.watch()
            consumer.accept(cap)
            cap.sync(entity)
        }
    }

    override fun hashCode(): Int {
        var result = activeThermalImaging.hashCode()
        result = 31 * result + (old?.hashCode() ?: 0)
        result = 31 * result + ammo.hashCode()
        return result
    }
}
