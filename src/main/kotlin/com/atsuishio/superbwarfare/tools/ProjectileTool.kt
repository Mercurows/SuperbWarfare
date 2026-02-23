package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.ProjectileTool.causeCustomExplode
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

fun ThrowableItemProjectile.customExplode(
    source: DamageSource?,
    target: Entity,
    damage: Float,
    radius: Float,
    damageMultiplier: Float = 0.0F
) = causeCustomExplode(this, source, target, damage, radius, damageMultiplier)

fun ThrowableItemProjectile.customExplode(
    target: Entity,
    damage: Float,
    radius: Float,
    damageMultiplier: Float = 0.0F
) = causeCustomExplode(this, target, damage, radius, damageMultiplier)

fun ThrowableItemProjectile.customExplode(
    damage: Float,
    radius: Float,
    damageMultiplier: Float = 0.0F
) = causeCustomExplode(this, damage, radius, damageMultiplier)

object ProjectileTool {
    @JvmStatic
    @JvmOverloads
    fun causeCustomExplode(
        projectile: ThrowableItemProjectile,
        source: DamageSource?,
        target: Entity,
        damage: Float,
        radius: Float,
        damageMultiplier: Float = 0.0f
    ) {
        val particleType = if (radius < 4) {
            ParticleTool.ParticleType.SMALL
        } else if (radius in 4.0..<10.0) {
            ParticleTool.ParticleType.MEDIUM
        } else if (radius in 10.0..<16.0) {
            ParticleTool.ParticleType.HUGE
        } else {
            ParticleTool.ParticleType.GIANT
        }

        CustomExplosion.Builder(projectile)
            .damageSource(source)
            .damage(damage)
            .radius(radius)
            .position(Vec3(target.x, target.y + 0.5 * target.bbHeight, target.z))
            .damageMultiplier(damageMultiplier)
            .withParticleType(particleType)
            .particlePosition(projectile.position().add(projectile.deltaMovement.scale(0.5)))
            .explode()

        val pos = projectile.position().add(projectile.deltaMovement.scale(0.5))

        if (projectile.level() is ServerLevel) {
            projectile.level().explode(
                source?.entity,
                pos.x,
                pos.y,
                pos.z,
                0.5f * radius,
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) Level.ExplosionInteraction.BLOCK else Level.ExplosionInteraction.NONE
            )
        }

        projectile.discard()
    }

    @JvmStatic
    @JvmOverloads
    fun causeCustomExplode(
        projectile: ThrowableItemProjectile,
        target: Entity,
        damage: Float,
        radius: Float,
        damageMultiplier: Float = 0.0f
    ) {
        causeCustomExplode(
            projectile,
            ModDamageTypes.causeCustomExplosionDamage(
                projectile.level().registryAccess(),
                projectile,
                projectile.owner
            ),
            target,
            damage,
            radius,
            damageMultiplier
        )
    }

    @JvmStatic
    @JvmOverloads
    fun causeCustomExplode(
        projectile: ThrowableItemProjectile,
        damage: Float,
        radius: Float,
        damageMultiplier: Float = 0.0f
    ) {
        causeCustomExplode(projectile, projectile, damage, radius, damageMultiplier)
    }
}
