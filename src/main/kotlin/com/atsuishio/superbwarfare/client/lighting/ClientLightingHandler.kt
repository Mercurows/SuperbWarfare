package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * Client-side bridge that routes projectile lifecycle events to the lighting system.
 *
 * <p>Isolates all {@code Minecraft} client imports from shared entity code so that
 * server-side classes never accidentally load client-only symbols.
 *
 * <p>Explosion radius is read directly from {@link IBulletProperties#getExplosionRadius()}
 * instead of a hardcoded {@code when} table — new projectile types are detected
 * automatically without any changes to this class.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object ClientLightingHandler {

    /**
     * Called every client tick from {@code ProjectileEntity} and
     * {@code FastThrowableProjectile#tick()}.
     *
     * @param entity the projectile entity being ticked
     */
    @JvmStatic
    fun handleProjectileTick(entity: Entity) {
        if (entity.tickCount == 1) {
            val owner = (entity as? Projectile)?.owner
            val localPlayer = Minecraft.getInstance().player

            if (owner !== localPlayer && owner is LivingEntity) {
                val params = MuzzleFlashHelper.calculateFromOwner(owner)

                // Use deltaMovement if available, otherwise use the owner's look angle
                val direction = if (entity.deltaMovement.lengthSqr() > 1e-6) {
                    entity.deltaMovement
                } else {
                    owner.lookAngle
                }

                if (params != null) {
                    MuzzleFlashHelper.spawnFlashCone(entity.position(), direction, params)
                }
            }

            val launchFlash = ProjectileLightHelper.getLaunchFlash(entity)
            if (launchFlash != null) {
                val direction = if (entity.deltaMovement.lengthSqr() > 1e-6) {
                    entity.deltaMovement
                } else {
                    (entity as? Projectile)
                        ?.owner?.lookAngle ?: entity.deltaMovement
                }
                MuzzleFlashHelper.spawnFlashCone(entity.position(), direction, launchFlash)
            }
        }

        ProjectileLightHelper.emitTrailLight(entity)
    }

    /**
     * Called when a projectile entity is removed from the client world.
     *
     * <p>Reads the explosion radius directly from {@link IBulletProperties} so
     * that every current and future projectile type is handled automatically —
     * no manual mapping table required.
     *
     * @param entity the projectile that was just removed
     */
    @JvmStatic
    fun handleProjectileRemoved(entity: Entity) {
        val radius = getExplosionRadius(entity)
        if (radius > 0f) {
            ProjectileLightHelper.emitExplosionFlashDirect(
                entity.level(), entity.position(), radius
            )
        }
    }

    /**
     * Returns the explosion light radius for a projectile.
     *
     * <p>Reads {@link IBulletProperties#getExplosionRadius()} directly from the
     * entity when available.  Non-explosive projectiles return 0 from that
     * method, so no additional filtering is necessary.
     *
     * @param entity the projectile entity
     * @return explosion radius in blocks, or {@code 0} if non-explosive
     */
    private fun getExplosionRadius(entity: Entity): Float =
        (entity as? IBulletProperties)?.getExplosionRadius() ?: 0f

    /**
     * Legacy entry point for direct explosion flash calls from
     * {@code ProjectileEntity#onHitBlock}.
     *
     * @param entity   the projectile that caused the explosion
     * @param location world-space impact position
     */
    @JvmStatic
    fun handleExplosionFlash(entity: Entity, location: Vec3) {
        ProjectileLightHelper.emitExplosionFlash(entity, location)
    }
}
