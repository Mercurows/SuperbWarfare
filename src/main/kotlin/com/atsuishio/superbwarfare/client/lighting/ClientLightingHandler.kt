package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * Client-side bridge that routes projectile lifecycle events to the lighting system.
 *
 * <p>Isolates all {@code Minecraft} client imports from shared entity code so that
 * server-side classes never accidentally load client-only symbols.
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
        ProjectileLightHelper.emitTrailLight(entity)
    }

    /**
     * Called when a projectile entity is added to the client world.
     *
     * @param entity the projectile that was just added to the client world
     */
    @JvmStatic
    fun handleProjectileAdded(entity: Entity) {
        val owner = (entity as? Projectile)?.owner
        val localPlayer = Minecraft.getInstance().player

        // Muzzle flash for other players' shots
        if (owner !== localPlayer && owner is LivingEntity) {
            val params = MuzzleFlashHelper.calculateFromOwner(owner)

            if (params != null) {
                val direction = if (entity.deltaMovement.lengthSqr() > 1e-6) {
                    entity.deltaMovement
                } else {
                    owner.lookAngle
                }
                MuzzleFlashHelper.spawnFlashCone(entity.position(), direction, params)
            }
        }

        // Launch backblast for rockets and large shells
        val launchFlash = ProjectileLightHelper.getLaunchFlash(entity)
        if (launchFlash != null) {
            val direction = if (entity.deltaMovement.lengthSqr() > 1e-6) {
                entity.deltaMovement
            } else {
                (entity as? Projectile)?.owner?.lookAngle ?: entity.deltaMovement
            }
            MuzzleFlashHelper.spawnFlashCone(entity.position(), direction, launchFlash)
        }
    }

    /**
     * Called when a projectile entity is removed from the client world.
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
     * @param entity the projectile entity
     * @return explosion radius in blocks, or {@code 0} if non-explosive
     */
    private fun getExplosionRadius(entity: Entity): Float =
        (entity as? IBulletProperties)?.getExplosionRadius() ?: 0f
}