package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModDataAttachments
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.util.INBTSerializable
import javax.annotation.ParametersAreNonnullByDefault

class PhosphorusFireCapability(var isOnFire: Boolean = false) : INBTSerializable<CompoundTag> {

    override fun serializeNBT(provider: HolderLookup.Provider) = CompoundTag().apply {
        putBoolean(TAG_PHOSPHORUS_FIRE, isOnFire)
    }

    @ParametersAreNonnullByDefault
    override fun deserializeNBT(provider: HolderLookup.Provider, nbt: CompoundTag) {
        if (nbt.contains(TAG_PHOSPHORUS_FIRE)) {
            this.isOnFire = nbt.getBoolean(TAG_PHOSPHORUS_FIRE)
        }
    }

    companion object {
        val ID: ResourceLocation = loc("phosphorus_fire_capability")
        const val TAG_PHOSPHORUS_FIRE: String = "SbwPhosphorusFire"

        @JvmStatic
        fun of(living: LivingEntity): PhosphorusFireCapability {
            return living.getData(ModDataAttachments.PHOSPHORUS_FIRE)
        }
    }
}
