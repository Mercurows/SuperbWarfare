package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.ProjectileTool.causeCustomExplode
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

fun Projectile.customExplode(
    source: DamageSource?,
    target: Entity,
    damage: Float,
    radius: Float
) = causeCustomExplode(this, source, target, damage, radius)

fun Projectile.customExplode(
    target: Entity,
    damage: Float,
    radius: Float
) = causeCustomExplode(this, target, damage, radius)

fun Projectile.customExplode(
    damage: Float,
    radius: Float
) = causeCustomExplode(this, damage, radius)

object ProjectileTool {
    @JvmStatic
    fun causeCustomExplode(
        projectile: Projectile,
        source: DamageSource?,
        target: Entity,
        damage: Float,
        radius: Float
    ) {
        val explosion = CustomExplosion.Builder(projectile)
            .damageSource(source)
            .damage(damage)
            .radius(radius)
            .position(Vec3(target.x, target.y + 0.5 * target.bbHeight, target.z))
            .particlePosition(projectile.position().add(projectile.deltaMovement.scale(0.5)))

        if (projectile is IBulletProperties) {
            explosion.beast(projectile.isBeast())
        }
        explosion.explode()

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
    fun causeCustomExplode(
        projectile: Projectile,
        target: Entity,
        damage: Float,
        radius: Float
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
            radius
        )
    }

    @JvmStatic
    fun causeCustomExplode(
        projectile: Projectile,
        damage: Float,
        radius: Float
    ) {
        causeCustomExplode(projectile, projectile, damage, radius)
    }
}
