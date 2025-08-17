package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ProjectileTool {

    public static void causeCustomExplode(ThrowableItemProjectile projectile, @Nullable DamageSource source, Entity target, float damage, float radius, float damageMultiplier) {
        ParticleTool.ParticleType particleType;
        if (radius <= 4) {
            particleType = ParticleTool.ParticleType.SMALL;
        } else if (radius > 4 && radius < 10) {
            particleType = ParticleTool.ParticleType.MEDIUM;
        } else if (radius >= 10 && radius < 16) {
            particleType = ParticleTool.ParticleType.HUGE;
        } else {
            particleType = ParticleTool.ParticleType.GIANT;
        }

        new CustomExplosion.Builder(projectile)
                .damageSource(source)
                .damage(damage)
                .radius(radius)
                .position(new Vec3(target.getX(), target.getY() + 0.5 * target.getBbHeight(), target.getZ()))
                .damageMultiplier(damageMultiplier)
                .withParticleType(particleType)
                .particlePosition(projectile.position().add(projectile.getDeltaMovement().scale(0.5)))
                .explode();

        Vec3 pos = projectile.position().add(projectile.getDeltaMovement().scale(0.5));

        if (projectile.level() instanceof ServerLevel) {
            projectile.level().explode(source == null ? null : source.getEntity(), pos.x, pos.y, pos.z, 0.5f * radius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        }

        projectile.discard();
    }

    public static void causeCustomExplode(ThrowableItemProjectile projectile, @Nullable DamageSource source, Entity target, float damage, float radius) {
        causeCustomExplode(projectile, source, target, damage, radius, 0.0f);
    }

    public static void causeCustomExplode(ThrowableItemProjectile projectile, Entity target, float damage, float radius) {
        causeCustomExplode(projectile, target, damage, radius, 0.0f);
    }

    public static void causeCustomExplode(ThrowableItemProjectile projectile, Entity target, float damage, float radius, float damageMultiplier) {
        causeCustomExplode(projectile, ModDamageTypes.causeCustomExplosionDamage(projectile.level().registryAccess(), projectile, projectile.getOwner()),
                target, damage, radius, damageMultiplier);
    }

    public static void causeCustomExplode(ThrowableItemProjectile projectile, float damage, float radius, float damageMultiplier) {
        causeCustomExplode(projectile, projectile, damage, radius, damageMultiplier);
    }

    public static void causeCustomExplode(ThrowableItemProjectile projectile, float damage, float radius) {
        causeCustomExplode(projectile, damage, radius, 0.0f);
    }

}
