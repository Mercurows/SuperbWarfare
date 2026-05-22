package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

open class Kv16Entity(type: EntityType<Kv16Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        val level = living?.level()
        if (level is ServerLevel) {
            val pos = getShootPos(living, 1f)
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD,
                    pos.x,
                    pos.y,
                    pos.z,
                    1, 0.1, 0.1, 0.1, 0.002, true)
        }
        super.vehicleShoot(living, uuid, targetPos)
    }
}
