package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

open class HappiestGhastEntity(type: EntityType<HappiestGhastEntity>, world: Level) : VehicleEntity(type, world) {
    override fun baseTick() {
        super.baseTick()

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (tickCount > 1) {
                ctx.playAnimation("animation.ghast.wave", AnimationPlayType.LOOP, fadeInTicks = 20)
            }
        }
    }

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        val serverLevel = level()
        if (serverLevel is ServerLevel) {
            val pos = getShootPos("Kalibr", 1f)
            val direct = getShootVec("Kalibr", 1f)
            missileLaunchEffect(serverLevel, pos, direct)
        }
        super.vehicleShoot(living, uuid, targetPos)
    }

    fun missileLaunchEffect(serverLevel: ServerLevel, pos: Vec3, direct: Vec3) {
        for (i in 0..10) {
            val s = 0 + 0.03 * i
            val position = pos.add(direct.scale(1.2 * i))
            Mod.queueServerWork(i) {
                ParticleTool.sendParticle(
                    serverLevel,
                    ParticleTypes.CLOUD,
                    position.x,
                    position.y,
                    position.z,
                    (1 + 1.3 * i).toInt(),
                    s,
                    s,
                    s,
                    0.002 * i,
                    true
                )
            }
        }

        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(0.4f, 0.4f, 0.4f, 45, 0.88f, 2, 0.05f),
            direct,
            pos.add(direct.scale(3.5)),
            0.15
        )
        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(0.45f, 0.45f, 0.45f, 47, 0.90f, 2, 0.03f),
            direct,
            pos.add(direct.scale(3.5)),
            0.125
        )
        ParticleTool.spawnDirectionalParticles(
            1,
            0.0,
            serverLevel,
            CannonMuzzleFlareOption(0.5f, 0.5f, 0.5f, 48, 0.92f, 2, 0.01f),
            direct,
            pos.add(direct.scale(3.5)),
            0.1
        )
    }
}
