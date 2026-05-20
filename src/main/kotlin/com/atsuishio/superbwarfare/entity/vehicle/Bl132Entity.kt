package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Bl132Entity(type: EntityType<Bl132Entity>, world: Level) : ArtilleryEntity(type, world), BasicGeoVehicleEntity {
//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
//        for (i in 1..4) {
//            "fire$i" {
//                if (barrelAnim.getOrElse(i) { 0 } > 0) {
//                    thenPlay("animation.bl_132.fire_${5 - i}")
//                } else {
//                    thenLoop("animation.bl_132.idle")
//                }
//            }
//        }
//    }

    override fun canBind() = true
}
