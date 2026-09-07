package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModDataAttachments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

@Serializable
data class PhosphorusFireCapability(
    @SerialName("SbwPhosphorusFire")
    val isOnFire: Boolean = false
) {

    companion object {
        val ID: ResourceLocation = loc("phosphorus_fire_capability")

        @JvmStatic
        fun get(entity: Entity): PhosphorusFireCapability {
            return entity.getData(ModDataAttachments.PHOSPHORUS_FIRE)
        }

        @JvmStatic
        fun set(entity: Entity, value: Boolean) {
            entity.setData(ModDataAttachments.PHOSPHORUS_FIRE, PhosphorusFireCapability(value))
        }

        @JvmStatic
        fun set(entity: Entity, value: PhosphorusFireCapability) {
            entity.setData(ModDataAttachments.PHOSPHORUS_FIRE, value)
        }
    }
}
