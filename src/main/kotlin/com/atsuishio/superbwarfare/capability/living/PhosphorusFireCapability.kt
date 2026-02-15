package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable

@AutoRegisterCapability
class PhosphorusFireCapability : INBTSerializable<CompoundTag> {
    var isOnFire: Boolean = false

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putBoolean(TAG_PHOSPHORUS_FIRE, this.isOnFire)
        return tag
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        if (nbt.contains(TAG_PHOSPHORUS_FIRE)) {
            this.isOnFire = nbt.getBoolean(TAG_PHOSPHORUS_FIRE)
        }
    }

    companion object {
        val ID: ResourceLocation = loc("phosphorus_fire_capability")
        const val TAG_PHOSPHORUS_FIRE: String = "SbwPhosphorusFire"

        @JvmStatic
        fun of(living: LivingEntity): PhosphorusFireCapability {
            return living.getCapability(ModCapabilities.PHOSPHORUS_FIRE_CAPABILITY)
                .orElseGet { PhosphorusFireCapability() }
        }
    }
}
