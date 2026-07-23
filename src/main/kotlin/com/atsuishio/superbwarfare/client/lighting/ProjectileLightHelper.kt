package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.entity.projectile.*
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic lighting for projectile flight trails and explosive events.
 *
 * Only projectiles with a visible engine exhaust or superheated surface
 * produce trail light.  Grenades, mines, and bullets do not glow in flight
 * but still produce explosion flashes on detonation.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object ProjectileLightHelper {
    private const val TRAIL_CULL_SQ = 128.0 * 128.0
    private const val EXPLODE_CULL_SQ = 192.0 * 192.0

    /**
     * @param level     peak light level per trail point
     * @param minLevel  minimum before expiry
     * @param ttl       lifetime in client ticks
     * @param radial    emit ambient glow around the projectile (rocket engines)
     */
    data class TrailLight(
        val level: Int,
        val minLevel: Int,
        val ttl: Int,
        val radial: Boolean = false
    )

    // -----------------------------------------------------------------
    // Trail classification — only things that visibly glow in flight
    // -----------------------------------------------------------------

    /**
     * Returns trail parameters for projectiles with a visible light source
     * (rocket exhaust, superheated metal).  Returns null for bullets,
     * grenades, mines, and other non-luminous projectiles.
     */
    @JvmStatic
    fun getTrailLight(entity: Entity): TrailLight? = when (entity) {
        // Guided missiles — rocket engine, brightest
        is MissileProjectile -> TrailLight(15, 12, 10, radial = true)

        // Unguided rockets — strong engine glow
        is MediumRocketEntity,
        is RpgRocketStandardEntity,
        is RpgRocketTBGEntity -> TrailLight(15, 11, 9, radial = true)

        // Small rockets
        is SmallRocketEntity -> TrailLight(14, 9, 7, radial = true)

        // Large cannon shells — superheated metal
        is CannonShellEntity -> TrailLight(13, 9, 6)

        // Small cannon shells
        is SmallCannonShellEntity -> TrailLight(11, 7, 5)

        // Mortar shells — visible hot trajectory
        is MortarShellEntity -> TrailLight(12, 8, 6)

        is GrapeshotEntity -> TrailLight(8, 6, 3)

        // Bullets, grenades, mines — no visible glow in flight
        else -> null
    }

    /**
     * Launch backblast parameters for rockets and large shells.
     */
    @JvmStatic
    fun getLaunchFlash(entity: Entity): MuzzleFlashHelper.FlashParams? = when (entity) {
        is MissileProjectile -> MuzzleFlashHelper.FlashParams(15, 12, 7)
        is MediumRocketEntity,
        is RpgRocketStandardEntity,
        is RpgRocketTBGEntity -> MuzzleFlashHelper.FlashParams(15, 11, 6)

        is SmallRocketEntity -> MuzzleFlashHelper.FlashParams(14, 9, 5)
        is CannonShellEntity -> MuzzleFlashHelper.FlashParams(15, 11, 5)
        is MortarShellEntity -> MuzzleFlashHelper.FlashParams(14, 9, 4)
        is GrapeshotEntity -> MuzzleFlashHelper.FlashParams(13, 10, 3)
        else -> null
    }

    // -----------------------------------------------------------------
    // Trail emission
    // -----------------------------------------------------------------

    private val ADJACENT_OFFSETS = arrayOf(
        intArrayOf(-1, 0, 0), intArrayOf(1, 0, 0),
        intArrayOf(0, -1, 0), intArrayOf(0, 1, 0),
        intArrayOf(0, 0, -1), intArrayOf(0, 0, 1)
    )

    /**
     * Emits trail light along the projectile's flight path.
     * Interpolates between previous and current position to fill gaps.
     * Rockets additionally get radial ambient glow from engine exhaust.
     */
    @JvmStatic
    fun emitTrailLight(entity: Entity) {
        val trail = getTrailLight(entity) ?: return
        val mc = mc
        val level = mc.level ?: return
        val player = mc.player ?: return

        val currentPos = entity.position()
        if (player.distanceToSqr(currentPos.x, currentPos.y, currentPos.z) > TRAIL_CULL_SQ) return

        val prevPos = Vec3(entity.xo, entity.yo, entity.zo)
        val distance = prevPos.distanceTo(currentPos)
        val engine = level.lightEngine

        // Place a light source at least every 1.5 blocks along the flight path
        val steps = 1.coerceAtLeast(ceil(distance / 1.5).toInt())
        for (i in 0 until steps) {
            val ratio = i.toDouble() / steps.toDouble()
            val point = prevPos.lerp(currentPos, ratio)
            val bp = BlockPos.containing(point.x, point.y, point.z)
            LightPositionRegistry.putSpark(bp.asLong(), trail.level, trail.minLevel, trail.ttl)
            engine.checkBlock(bp)
        }

        // Radial ambient glow for rocket engines
        if (trail.radial) {
            val centerBp = BlockPos.containing(currentPos.x, currentPos.y, currentPos.z)
            val radialLevel = (trail.level - 2).coerceAtLeast(8)
            val radialMin = (trail.minLevel - 2).coerceAtLeast(4)
            val radialTtl = (trail.ttl - 1).coerceAtLeast(3)

            ADJACENT_OFFSETS.forEach { offset ->
                val bp = centerBp.offset(offset[0], offset[1], offset[2])
                LightPositionRegistry.putSpark(bp.asLong(), radialLevel, radialMin, radialTtl)
                engine.checkBlock(bp)
            }
        }
    }

    // -----------------------------------------------------------------
    // Explosion flash — all explosive projectiles
    // -----------------------------------------------------------------

    /**
     * Spawns a bright, large-radius explosion flash that snaps off sharply.
     *
     * <p>Design principles:
     * <ul>
     *   <li><b>Large spread</b> — nodes at up to {@code radius * 0.9} blocks fill
     *       the entire blast area with light.</li>
     *   <li><b>Uniform short TTL</b> — every node shares the same 3–4 tick
     *       lifetime, so all nodes expire simultaneously.  No staggered fade,
     *       no leftover light fragments.</li>
     *   <li><b>minLevel = maxLevel − 1</b> — light stays at peak until the final
     *       tick, then snaps off instantly instead of fading.</li>
     * </ul>
     *
     * <p>Node layout (all at the same TTL):
     * <ol>
     *   <li>Center node — level 15</li>
     *   <li>Inner hex ring — 6 nodes at {@code radius * 0.45} blocks</li>
     *   <li>Outer hex ring — 6 nodes at {@code radius * 0.9} blocks
     *       (only when {@code radius >= 4})</li>
     * </ol>
     *
     * @param level   the client level
     * @param center  world-space explosion center
     * @param radius  logical explosion radius in blocks
     */
    @JvmStatic
    fun emitExplosionFlashDirect(level: Level, center: Vec3, radius: Float) {
        if (radius <= 0f) return
        val player = localPlayer ?: return
        if (player.distanceToSqr(center.x, center.y, center.z) > EXPLODE_CULL_SQ) return

        val bp = BlockPos.containing(center.x, center.y, center.z)
        val engine = level.lightEngine

        // All nodes share one TTL so they expire on the exact same tick.
        // 3 ticks for small blasts, 4 for large — both feel instant.
        val ttl = if (radius >= 5f) 4 else 3

        // minLevel = maxLevel - 1: no smooth fade, just a snap-off on expiry.
        // --- Layer 1: center ---
        LightPositionRegistry.putSpark(bp.asLong(), 15, 14, ttl)
        engine.checkBlock(bp)

        // --- Layer 2: inner hex ring ---
        val innerDist = (radius * 0.45).coerceAtLeast(1.5)
        spawnExplosionHexRing(center, bp, engine, innerDist, 15, 14, ttl)

        // --- Layer 3: outer hex ring (large blasts only) ---
        if (radius >= 4f) {
            val outerDist = radius * 0.9
            spawnExplosionHexRing(center, bp, engine, outerDist, 14, 13, ttl)
        }
    }

    /**
     * Places 6 light nodes in a flat hexagonal ring around the explosion center.
     *
     * <p>The ring lies in the XZ plane at {@code center.y} — explosions are
     * typically near the ground, so a horizontal ring gives maximum visibility.
     *
     * @param center    world-space explosion center
     * @param centerBp  pre-computed [BlockPos] of [center] (used for dedup check)
     * @param engine    client-side light engine
     * @param ringDist  radial distance from the center in blocks
     * @param maxLevel  peak light level for ring nodes
     * @param minLevel  minimum light level for ring nodes
     * @param ttl       lifetime in client ticks
     */
    private fun spawnExplosionHexRing(
        center: Vec3,
        centerBp: BlockPos,
        engine: LevelLightEngine,
        ringDist: Double,
        maxLevel: Int,
        minLevel: Int,
        ttl: Int
    ) {
        for (i in 0 until 6) {
            val angle = i * (Math.PI / 3.0)
            val ringBp = BlockPos.containing(
                center.x + ringDist * cos(angle),
                center.y,
                center.z + sin(angle) * ringDist
            )
            // Skip if this maps to the same block as the center node
            if (ringBp == centerBp) continue
            LightPositionRegistry.putSpark(ringBp.asLong(), maxLevel, minLevel, ttl)
            engine.checkBlock(ringBp)
        }
    }

    /**
     * Derives explosion radius from {@link IBulletProperties} and delegates to
     * {@link #emitExplosionFlashDirect}.
     *
     * <p>Uses the interface directly so all current and future projectile types
     * are handled automatically — no manual type mapping needed.
     *
     * @param entity the projectile that exploded
     * @param center world-space explosion center
     */
    @JvmStatic
    fun emitExplosionFlash(entity: Entity, center: Vec3) {
        val radius = (entity as? IBulletProperties)?.getExplosionRadius() ?: 0f
        if (radius > 0f) {
            emitExplosionFlashDirect(entity.level(), center, radius)
        }
    }
}
