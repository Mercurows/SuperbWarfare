package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.maydaymemory.mae.basic.Pose
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

class VehicleAnimationInstance<T>(entity: T) where T : Entity, T : BasicGeoVehicleEntity {
    val context: VehicleAnimationContext<T>

    init {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        context = VehicleAnimationContext(entity, ResourceLocation(namespace, id))
    }

    fun fire(weaponName: String) {
        context.playAnimation("animation.$weaponName.fire", AnimationPlayType.PLAY_ONCE_STOP)
    }

    fun tick() {
        context.tick()
    }

    fun getPose(): Pose {
        return context.getPose()
    }
}
