package com.atsuishio.superbwarfare.client.animation.entity

import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.maydaymemory.mae.basic.Pose
import net.minecraft.resources.ResourceLocation

open class VehicleAnimationInstance<T>(
    entity: T,
    location: ResourceLocation
) where T : VehicleEntity, T : BasicGeoVehicleEntity {
    val context: VehicleAnimationContext<T> = VehicleAnimationContext(entity, location)

    open fun fire(weaponName: String, index: Int) {
        context.fire(weaponName, index)
    }

    open fun tick() {
        context.tick()
    }

    open fun getPose(): Pose {
        return context.getPose()
    }

    companion object {
        @JvmStatic
        fun <T> create(entity: T): VehicleAnimationInstance<T>? where T : VehicleEntity, T : BasicGeoVehicleEntity {
            val anim = VehicleResource.compute(entity)
            val location = anim.animation ?: return null
            return VehicleAnimationInstance(entity, location)
        }
    }
}
