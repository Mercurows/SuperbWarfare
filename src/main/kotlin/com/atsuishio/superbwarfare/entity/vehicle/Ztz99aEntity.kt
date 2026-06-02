package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

class Ztz99aEntity(type: EntityType<Ztz99aEntity>, world: Level) : VehicleEntity(type, world) {
    override var turretXRot = -3f
    override var turretXRotO = -3f

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
