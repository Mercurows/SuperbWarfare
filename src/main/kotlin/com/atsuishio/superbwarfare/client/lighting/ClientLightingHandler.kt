package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import net.minecraft.world.entity.Entity
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
     * 信号弹（FlareDecoyEntity）每 tick 的跟随光：暖白、大半径、无遮挡 march。
     * 之前信号弹只靠贴图自发光，完全没有世界光照。
     */
    @JvmStatic
    fun handleFlareTick(entity: Entity) {
        WorldLightHandler.put(
            "flare_${entity.id}",
            entity.position().add(0.0, entity.bbHeight * 0.5, 0.0),
            Vec3.ZERO,
            strength = 0.9f,
            radius = 10f,
            durationTicks = 2,
            color = Vec3(1.0, 0.86, 0.72),
        )
    }

    /**
     * Called when a projectile entity is added to the client world.
     *
     * @param entity the projectile that was just added to the client world
     */
    @JvmStatic
    fun handleProjectileAdded(entity: Entity) {
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
        // 弹丸消失后立即移除跟随光（曳光主光 + 尾焰光）。
        ProjectileLightHelper.clear(entity)

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