package com.atsuishio.superbwarfare.capability.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.ModCapabilities
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable

@AutoRegisterCapability
class InfinityAmmoCapability(var hasInfinityAmmo: Boolean = false) : INBTSerializable<CompoundTag> {
    override fun serializeNBT() = CompoundTag().apply {
        putBoolean(TAG_INFINITY_AMMO, hasInfinityAmmo)
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        if (nbt.contains(TAG_INFINITY_AMMO)) {
            this.hasInfinityAmmo = nbt.getBoolean(TAG_INFINITY_AMMO)
        }
    }

    companion object {
        val ID = Mod.loc("infinity_ammo_capability")
        const val TAG_INFINITY_AMMO = "SbwInfinityAmmo"

        @JvmStatic
        fun get(entity: Entity): InfinityAmmoCapability {
            return entity.getCapability(ModCapabilities.INFINITY_AMMO_CAPABILITY)
                .orElseGet { InfinityAmmoCapability() }
        }

        @JvmStatic
        fun modify(entity: Entity, modifier: (InfinityAmmoCapability) -> Unit) {
            val data = get(entity)
            data.apply(modifier)
        }
    }
}