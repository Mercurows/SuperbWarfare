package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModDataAttachments
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.common.util.INBTSerializable

class InfinityAmmoCapability(var hasInfinityAmmo: Boolean = false) : INBTSerializable<CompoundTag> {

    override fun serializeNBT(provider: HolderLookup.Provider) = CompoundTag().apply {
        putBoolean(TAG_INFINITY_AMMO, hasInfinityAmmo)
    }

    override fun deserializeNBT(provider: HolderLookup.Provider, nbt: CompoundTag) {
        if (nbt.contains(TAG_INFINITY_AMMO)) {
            this.hasInfinityAmmo = nbt.getBoolean(TAG_INFINITY_AMMO)
        }
    }

    companion object {
        val ID = loc("infinity_ammo_capability")
        const val TAG_INFINITY_AMMO = "SbwInfinityAmmo"

        @JvmStatic
        fun get(entity: Entity): InfinityAmmoCapability {
            return entity.getData(ModDataAttachments.INFINITY_AMMO)
        }

        @JvmStatic
        fun modify(entity: Entity, modifier: (InfinityAmmoCapability) -> Unit) {
            val data = get(entity)
            data.apply(modifier)
            entity.setData(ModDataAttachments.INFINITY_AMMO, data)
        }
    }
}
