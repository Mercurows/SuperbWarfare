package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.Mod
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
     * <p>Muzzle flash emission lives in {@link #handleProjectileAdded} — this
     * method only maintains the flight trail light.
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
     * <p>This hook is used instead of {@code tickCount == 1} because fast projectiles
     * (e.g. sniper bullets) are frequently spawned and removed by the server within
     * the same client packet batch. In that case the entity is added and removed
     * without ever being ticked, so tick-based flash emission is silently skipped.
     * {@code onAddedToWorld()} is guaranteed to fire exactly once for every
     * projectile the client ever sees.
     *
     * <p>Ordering guarantees at this point ({@code ClientPacketListener.handleAddEntity}):
     * <ul>
     *   <li>{@code Projectile.recreateFromPacket} has already resolved the owner
     *       from the spawn packet's data field — {@code getOwner()} is reliable.</li>
     *   <li>The owner's equipment was synced while the client was tracking them,
     *       so {@code mainHandItem} is valid for any visible shooter.</li>
     *   <li>{@code SynchedEntityData} has NOT been applied yet — do not read
     *       entity data accessors here.</li>
     * </ul>
     *
     * @param entity the projectile that was just added to the client world
     */
    @JvmStatic
    fun handleProjectileAdded(entity: Entity) {
        val owner = (entity as? Projectile)?.owner
        val localPlayer = Minecraft.getInstance().player

        // Mod.LOGGER.info(
        //     "[FlashLight] added: id={} type={} tickCount={} owner={} isLocal={} pos={}",
        //     entity.id,
        //     entity.type.description.string,
        //     entity.tickCount,
        //     owner?.name?.string ?: "NULL",
        //     owner === localPlayer,
        //     entity.position()
        // )

        // Muzzle flash for other players' shots
        if (owner !== localPlayer && owner is LivingEntity) {
            val params = MuzzleFlashHelper.calculateFromOwner(owner)

            if (params != null) {
                // Spawn-packet velocity is clamped to ±3.9 blocks/tick, so fast
                // bullets may have unreliable deltaMovement here — fall back to
                // the owner's look angle in that case
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
