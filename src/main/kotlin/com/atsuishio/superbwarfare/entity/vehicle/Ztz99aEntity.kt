package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animation.AnimatableManager
import java.util.*

class Ztz99aEntity(type: EntityType<Ztz99aEntity>, world: Level) : GeoVehicleEntity(type, world) {
    override var turretXRot = -3f
    override var turretXRotO = -3f
    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.3f) * damage }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        "cannon" {
            if (getShootAnimationTimer(0, 0) > 0) {
                thenPlay("animation.t_90a.fire")
            } else {
                thenLoop("animation.t_90a.idle")
            }
        }
        "coax" {
            if (getShootAnimationTimer(0, 1) > 0) {
                thenPlay("animation.t_90a.fire_coax")
            } else {
                thenLoop("animation.t_90a.idle_coax")
            }
        }
        "passengerWeaponStation" {
            if (getShootAnimationTimer(1, 0) > 0) {
                thenPlay("animation.t_90a.fire_weapon_station")
            } else {
                thenLoop("animation.t_90a.idle_weapon_station")
            }
        }
    }

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
