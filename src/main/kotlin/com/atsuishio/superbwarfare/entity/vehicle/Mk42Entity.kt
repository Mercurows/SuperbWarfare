package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Mk42Entity(type: EntityType<Mk42Entity>, world: Level) : ArtilleryEntity(type, world), BasicGeoVehicleEntity {
//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
//        "shoot" {
//            if (getShootAnimationTimer(0, 0) > 0) {
//                thenPlay("animation.mk_42.fire")
//            } else {
//                thenLoop("animation.mk_42.idle")
//            }
//        }
//    }

    override fun canBind() = true
}
