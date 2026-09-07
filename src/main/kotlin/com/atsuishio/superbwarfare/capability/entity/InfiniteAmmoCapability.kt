package com.atsuishio.superbwarfare.capability.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.serialization.decodeFromCompoundTag
import com.atsuishio.superbwarfare.serialization.encodeToCompoundTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable

@AutoRegisterCapability
@Serializable
data class InfiniteAmmoCapability(
    @SerialName("SbwInfiniteAmmo")
    var hasInfiniteAmmo: Boolean = false,
) : INBTSerializable<CompoundTag> {

    override fun serializeNBT(): CompoundTag =
        encodeToCompoundTag(serializer<InfiniteAmmoCapability>(), this)

    override fun deserializeNBT(nbt: CompoundTag) {
        hasInfiniteAmmo = decodeFromCompoundTag(serializer<InfiniteAmmoCapability>(), nbt).hasInfiniteAmmo
    }

    companion object {
        val ID = Mod.loc("infinite_ammo_capability")

        @JvmStatic
        fun get(entity: Entity): InfiniteAmmoCapability {
            return entity.getCapability(ModCapabilities.INFINITE_AMMO_CAPABILITY)
                .orElseGet { InfiniteAmmoCapability() }
        }

        @JvmStatic
        fun set(entity: Entity, value: Boolean) {
            get(entity).hasInfiniteAmmo = value
        }

        @JvmStatic
        fun toggle(entity: Entity): Boolean {
            val enabled = !get(entity).hasInfiniteAmmo
            set(entity, enabled)
            return enabled
        }
    }
}
