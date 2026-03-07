package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animation.AnimatableManager
import java.util.*

class Ju87Entity(type: EntityType<Ju87Entity>, world: Level) : GeoVehicleEntity(type, world) {

    override var turretYRot = 180f
    override var turretYRotO = 180f

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        val level = living?.level()
        if (level is ServerLevel && living == firstPassenger && getWeaponIndex(0) == 0) {
            val pos = getShootPos(living, 1f)
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD,
                    pos.x,
                    pos.y,
                    pos.z,
                    1, 0.1, 0.1, 0.1, 0.0, true)
        }
        super.vehicleShoot(living, uuid, targetPos)
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        "machineGun" {
            if (getShootAnimationTimer(1, 0) > 0) {
                thenPlay("animation.mg_17.fire")
            } else {
                thenLoop("animation.mg_17.idle")
            }
        }
    }
}
