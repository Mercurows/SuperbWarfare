package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModDataAttachments
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.common.util.INBTSerializable

class InfiniteAmmoCapability(var hasInfiniteAmmo: Boolean = false) : INBTSerializable<CompoundTag> {

    override fun serializeNBT(provider: HolderLookup.Provider) = CompoundTag().apply {
        putBoolean(TAG_INFINITE_AMMO, hasInfiniteAmmo)
    }

    override fun deserializeNBT(provider: HolderLookup.Provider, nbt: CompoundTag) {
        if (nbt.contains(TAG_INFINITE_AMMO)) {
            this.hasInfiniteAmmo = nbt.getBoolean(TAG_INFINITE_AMMO)
        }
    }

    companion object {
        val ID = loc("infinite_ammo_capability")
        const val TAG_INFINITE_AMMO = "SbwInfiniteAmmo"

        @JvmStatic
        fun get(entity: Entity): InfiniteAmmoCapability {
            return entity.getData(ModDataAttachments.INFINITE_AMMO)
        }

        @JvmStatic
        fun modify(entity: Entity, modifier: (InfiniteAmmoCapability) -> Unit) {
            val data = get(entity)
            data.apply(modifier)
            entity.setData(ModDataAttachments.INFINITE_AMMO, data)
        }
    }
}
