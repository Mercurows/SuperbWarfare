package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class SpeedboatEntity(type: EntityType<SpeedboatEntity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {

//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
//        "machineGun" {
//            if (getShootAnimationTimer(0, 0) > 0) {
//                thenPlay("animation.speedboat.fire")
//            } else {
//                thenLoop("animation.speedboat.idle")
//            }
//        }
//    }
}
