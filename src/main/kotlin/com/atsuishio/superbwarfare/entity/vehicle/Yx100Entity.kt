package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Yx100Entity(type: EntityType<Yx100Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
//        "cannon" {
//            if (getShootAnimationTimer(0, 0) > 0) {
//                thenPlay("animation.yx_100.fire")
//            } else {
//                thenLoop("animation.yx_100.idle")
//            }
//        }
//        "coax" {
//            if (getShootAnimationTimer(0, 1) > 0) {
//                thenPlay("animation.yx_100.fire_coax")
//            } else {
//                thenLoop("animation.yx_100.idle_coax")
//            }
//        }
//        "passengerWeaponStation" {
//            if (getShootAnimationTimer(1, 0) > 0) {
//                thenPlay("animation.yx_100.fire_weapon_station")
//            } else {
//                thenLoop("animation.yx_100.idle_weapon_station")
//            }
//        }
//    }

    override fun getTrackAnimationLength() = 80
    override fun getTurretMaxHealth() = 200f
    override fun getWheelMaxHealth() = 200f
    override fun getEngineMaxHealth() = 300f

    override val customTurretMinPitch: Float
        get() = if (Mth.abs(turretYRot) > 135) ((Mth.abs(turretYRot) - 135) * 0.5f).coerceAtMost(5f) else 0f
}
