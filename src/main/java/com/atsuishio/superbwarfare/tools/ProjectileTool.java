package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;

public class ProjectileTool {

    public static void causeCustomExplode(ThrowableItemProjectile projectile, @Nullable DamageSource source, Entity target, float damage, float radius, float damageMultiplier) {
        CustomExplosion explosion = new CustomExplosion(projectile.level(), projectile, source, damage,
                target.getX(),
                target.getY() + 0.5 * target.getBbHeight(),
                target.getZ(),
                radius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(damageMultiplier);
        explosion.explode();
        EventHooks.onExplosionStart(projectile.level(), explosion);
        explosion.finalizeExplosion(false);

        if (radius <= 5) {
            ParticleTool.spawnSmallExplosionParticles(projectile.level(), projectile.position().add(projectile.getDeltaMovement().scale(0.5)));
        } else if (radius > 5 && radius < 10) {
            ParticleTool.spawnMediumExplosionParticles(projectile.level(), projectile.position().add(projectile.getDeltaMovement().scale(0.5)));
        } else {
            ParticleTool.spawnHugeExplosionParticles(projectile.level(), projectile.position().add(projectile.getDeltaMovement().scale(0.5)));
        }

        Vec3 pos = projectile.position().add(projectile.getDeltaMovement().scale(0.5));

        if (projectile.level() instanceof ServerLevel) {
            projectile.level().explode(source == null ? null : source.getEntity(), pos.x, pos.y, pos.z, 0.5f * radius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        }

        projectile.discard();
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
