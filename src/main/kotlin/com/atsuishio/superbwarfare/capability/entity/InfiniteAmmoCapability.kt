package com.atsuishio.superbwarfare.capability.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModDataAttachments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.Entity

@Serializable
data class InfiniteAmmoCapability(
    @SerialName("SbwInfiniteAmmo")
    val hasInfiniteAmmo: Boolean = false
) {

    companion object {
        val ID = Mod.loc("infinite_ammo_capability")

        @JvmStatic
        fun get(entity: Entity): InfiniteAmmoCapability {
            return entity.getData(ModDataAttachments.INFINITE_AMMO)
        }

        @JvmStatic
        fun set(entity: Entity, value: Boolean) {
            set(entity, InfiniteAmmoCapability(value))
        }

        @JvmStatic
        fun set(entity: Entity, value: InfiniteAmmoCapability) {
            entity.setData(ModDataAttachments.INFINITE_AMMO, value)
        }

        @JvmStatic
        fun toggle(entity: Entity): Boolean {
            val enabled = !get(entity).hasInfiniteAmmo
            set(entity, enabled)
            return enabled
        }
    }
}