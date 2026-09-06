package com.atsuishio.superbwarfare.capability.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.ModCapabilities
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable

@AutoRegisterCapability
class InfiniteAmmoCapability(var hasInfiniteAmmo: Boolean = false) : INBTSerializable<CompoundTag> {
    override fun serializeNBT() = CompoundTag().apply {
        putBoolean(TAG_INFINITE_AMMO, hasInfiniteAmmo)
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        if (nbt.contains(TAG_INFINITE_AMMO)) {
            this.hasInfiniteAmmo = nbt.getBoolean(TAG_INFINITE_AMMO)
        }
    }

    companion object {
        val ID = Mod.loc("infinite_ammo_capability")
        const val TAG_INFINITE_AMMO = "SbwInfiniteAmmo"

        @JvmStatic
        fun get(entity: Entity): InfiniteAmmoCapability {
            return entity.getCapability(ModCapabilities.INFINITE_AMMO_CAPABILITY)
                .orElseGet { InfiniteAmmoCapability() }
        }

        @JvmStatic
        fun modify(entity: Entity, modifier: (InfiniteAmmoCapability) -> Unit) {
            val data = get(entity)
            data.apply(modifier)
        }
    }
}