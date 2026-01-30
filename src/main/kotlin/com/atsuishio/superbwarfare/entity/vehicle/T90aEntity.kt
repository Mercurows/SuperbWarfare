package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

class T90aEntity(type: EntityType<T90aEntity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.3f) * damage }

//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
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

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        val level = living?.level()
        if (level is ServerLevel && living == firstPassenger && getWeaponIndex(0) == 0) {
            ParticleTool.spawnBigCannonMuzzleParticles(getShootVec(living, 1f), getShootPos(living, 1f), level, this)
        }
        super.vehicleShoot(living, uuid, targetPos)
    }

    override fun getTurretMaxHealth() = 100f
    override fun getWheelMaxHealth() = 100f
    override fun getEngineMaxHealth() = 150f
}
