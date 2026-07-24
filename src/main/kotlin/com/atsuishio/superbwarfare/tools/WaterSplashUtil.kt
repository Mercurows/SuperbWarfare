package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.VectorTool.randomSpreadVec
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared water/lava impact effects for all projectile types.
 *
 * <p>Scales splash intensity by projectile damage — pistol rounds create a subtle ripple,
 * cannon shells throw up a dramatic water column with chaotic drop arcs and lingering mist
 * of varied sizes.
 *
 * <p>Particle layer breakdown (lowest to highest power threshold):
 * <ul>
 *   <li>Foam     — tiny surface bubbles, power &gt; {@value FOAM_THRESHOLD}</li>
 *   <li>Drops    — individual SPLASH arcs with per-particle velocity, always present</li>
 *   <li>Flash    — brief CustomFlare surface highlight, power &gt; {@value FLASH_THRESHOLD}</li>
 *   <li>Column   — upward water column + upward CustomCloud, power &gt; {@value COLUMN_THRESHOLD}</li>
 *   <li>Rings    — expanding ring layers with positional jitter, always present</li>
 *   <li>Mist     — varied-size CustomCloud billows, power &gt; {@value MIST_THRESHOLD}</li>
 * </ul>
 *
 * @author SuperbWarfare contributors
 * @since 0.8.9.1
 */
object WaterSplashUtil {

    //
    // Tuning constants — change numbers here, not inside logic
    //

    /** Damage normalisation divisor for [power] calculation. */
    private const val POWER_DAMAGE_SCALE = 30f

    /** Minimum power clamp — even weak projectiles produce a small splash. */
    private const val POWER_MIN = 0.1f

    /** Maximum power clamp. */
    private const val POWER_MAX = 1.0f

    /** Minimum power required to spawn foam particles. */
    private const val FOAM_THRESHOLD = 0.15f

    /** Minimum power required to spawn a surface flash. */
    private const val FLASH_THRESHOLD = 0.70f

    /** Minimum power required to spawn a vertical water column. */
    private const val COLUMN_THRESHOLD = 0.60f

    /** Minimum power required to spawn mist clouds. */
    private const val MIST_THRESHOLD = 0.30f

    /** Velocity damping applied to projectile upon water entry (non-discarded path). */
    private val WATER_ENTRY_DAMPING = Vec3(0.1, 0.1, 0.1)

    //
    // Public API
    //

    /**
     * Spawns water or lava impact particles and sound at [location].
     *
     * @param level           server level in which to spawn particles and play sounds.
     * @param projectile      the impacting projectile entity.
     * @param location        world-space impact point.
     * @param result          block hit result providing face direction and block position.
     * @param damage          projectile damage value — controls splash scale.
     * @param discardOnWater  if `true`, discard the projectile immediately after water hit;
     *                        otherwise slow it down and spawn underwater bubbles.
     * @return `true` if a fluid surface (water or lava) was hit.
     */
    @JvmStatic
    fun handleFluidImpact(
        level: ServerLevel,
        projectile: Projectile,
        location: Vec3,
        result: BlockHitResult,
        damage: Float,
        discardOnWater: Boolean = false
    ): Boolean {
        val pos = result.blockPos
        val face = result.direction
        val state = level.getBlockState(pos)

        // Blend face normal with inverse projectile direction for a natural outward splash angle
        val dir = Vec3(
            face.stepX.toDouble(),
            face.stepY.toDouble(),
            face.stepZ.toDouble()
        ).add(projectile.deltaMovement.normalize().scale(-0.1))

        return when {
            state.block === Blocks.WATER && !projectile.isInWater -> {
                spawnWaterSplash(level, projectile, location, dir, damage)
                if (discardOnWater) {
                    projectile.discard()
                } else {
                    projectile.deltaMovement = projectile.deltaMovement.multiply(WATER_ENTRY_DAMPING)
                    spawnUnderwaterBubbles(level, projectile, location)
                }
                true
            }

            state.block === Blocks.LAVA && !projectile.isInLava -> {
                spawnLavaSplash(level, location, dir, state)
                projectile.discard()
                true
            }

            else -> false
        }
    }

    //
    // Water splash — main orchestrator
    //

    /**
     * Orchestrates all water splash particle layers scaled by [damage].
     *
     * @param level      server level.
     * @param projectile impacting projectile (used for velocity on underwater path).
     * @param location   world-space impact point.
     * @param dir        outward splash direction (face normal blended with projectile delta).
     * @param damage     raw projectile damage for scale computation.
     */
    private fun spawnWaterSplash(
        level: ServerLevel,
        projectile: Projectile,
        location: Vec3,
        dir: Vec3,
        damage: Float
    ) {
        val rng = level.random
        val power = (damage / POWER_DAMAGE_SCALE).coerceIn(POWER_MIN, POWER_MAX)

        spawnFoamLayer(level, location, power)
        spawnDropLayer(level, location, power)
        if (power >= FLASH_THRESHOLD) spawnSurfaceFlash(level, location, power)
        if (power >= COLUMN_THRESHOLD) spawnColumnLayer(level, location, power)
        spawnRingLayers(level, location, power)
        if (power >= MIST_THRESHOLD) spawnVariedMist(level, location, power)

        // Ambient rain/drizzle above the impact point
        if (power > MIST_THRESHOLD) {
            ParticleTool.sendParticle(
                level, ParticleTypes.RAIN,
                location.x, location.y + 0.25, location.z,
                (power * 6).toInt().coerceIn(0, 6),
                0.1 + 0.3 * power, 0.15, 0.1 + 0.3 * power,
                0.05,
                true
            )
        }

        playWaterSound(level, location, power)
    }

    //
    // Individual layers
    //

    /**
     * Spawns tiny foam bubbles at the water surface.
     *
     * <p>Each particle is sent individually with {@code count = 0} so that
     * dx/dy/dz are interpreted as direct velocity components rather than a
     * random spread box — this produces the chaotic micro-scatter of real foam.
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnFoamLayer(level: ServerLevel, location: Vec3, power: Float) {
        if (power < FOAM_THRESHOLD) return
        val rng = level.random
        val foamCount = (power * 7).toInt().coerceIn(1, 7)

        repeat(foamCount) {
            // Randomise size so foam looks "bubbly" rather than uniform
            val foamSize = (0.06f + 0.10f * power) * (0.4f + 0.6f * rng.nextFloat())
            val foamLife = (15 + (20 * power * rng.nextFloat()).toInt()).coerceAtLeast(8)

            val foam = CustomCloudOption(
                0.88f + 0.08f * rng.nextFloat(),
                0.93f + 0.05f * rng.nextFloat(),
                0.98f,
                foamLife,
                foamSize,
                0.0f,          // no gravity — stays flat on surface
                cooldown = false,
                light = false
            )

            // Lateral scatter, barely any vertical lift
            val angle = rng.nextDouble() * 2.0 * PI
            val lateralMag = (0.03 + 0.07 * power) * rng.nextDouble()
            ParticleTool.sendParticle(
                level, foam,
                location.x + rng.triangle(0.0, 0.08 * power),
                location.y + 0.01,
                location.z + rng.triangle(0.0, 0.08 * power),
                0,                                       // count=0 → use velocity directly
                cos(angle) * lateralMag,
                0.005 + 0.01 * rng.nextDouble(),
                sin(angle) * lateralMag,
                1.0,
                true
            )
        }
    }

    /**
     * Spawns individual SPLASH drop particles, each with its own computed polar trajectory.
     *
     * <p>Sends each drop as a single particle ({@code count = 0}), passing the
     * pre-computed velocity vector instead of a random spread box.  This produces
     * natural parabolic arcs that differ from one another — avoiding the
     * "uniform dome" appearance of a single multi-count call.
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnDropLayer(level: ServerLevel, location: Vec3, power: Float) {
        val rng = level.random
        val dropCount = (3 + power * 11).toInt().coerceIn(3, 14)

        repeat(dropCount) {
            // Random azimuth (full 360°) and elevation (0° = horizontal → ~70° = steep upward)
            val azimuth = rng.nextDouble() * 2.0 * PI
            // Bias elevation toward the upper half with square-root to avoid a flat carpet of drops
            val elevNorm = rng.nextDouble().let { Math.sqrt(it) }           // [0,1], skewed high
            val elevation = elevNorm * (PI / 2.6)                           // 0 → ~69°

            val lateralSpeed = (0.15 + 0.45 * power) * (0.4 + 0.6 * rng.nextDouble())
            val verticalSpeed = (0.15 + 0.65 * power) * (0.5 + 0.5 * rng.nextDouble())

            val vx = cos(azimuth) * lateralSpeed * cos(elevation)
            val vy = verticalSpeed * sin(elevation)
            val vz = sin(azimuth) * lateralSpeed * cos(elevation)

            // Slight spawn position jitter so drops don't all originate from a single point
            ParticleTool.sendParticle(
                level, ParticleTypes.SPLASH,
                location.x + rng.triangle(0.0, 0.04),
                location.y + 0.05,
                location.z + rng.triangle(0.0, 0.04),
                0,        // count=0 → velocity interpreted as direct vector
                vx, vy, vz,
                1.0,
                true
            )
        }
    }

    /**
     * Spawns a brief soft-glow surface highlight (CustomFlare) for heavy impacts.
     *
     * <p>The flare quickly expands and fades ({@code fade ≈ 0.65}), simulating
     * the bright momentary glint of water as a heavy shell strikes it.
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnSurfaceFlash(level: ServerLevel, location: Vec3, power: Float) {
        val rng = level.random
        val flashSize = 0.04f + 0.06f * (power - FLASH_THRESHOLD) / (POWER_MAX - FLASH_THRESHOLD)

        val flash = CustomFlareOption(
            0.80f + 0.15f * rng.nextFloat(),  // warm white-blue
            0.88f + 0.10f * rng.nextFloat(),
            1.00f,
            life = 8,
            fade = 0.60f,                     // fast fade — purely momentary
            animationSpeed = 2,
            sizeAdd = 0.012f,                 // subtle outward bloom
            size = flashSize
        )

        ParticleTool.sendParticle(
            level, flash,
            location.x + rng.triangle(0.0, 0.06),
            location.y + 0.02,
            location.z + rng.triangle(0.0, 0.06),
            0,
            rng.triangle(0.0, 0.005),
            0.005,
            rng.triangle(0.0, 0.005),
            1.0,
            true
        )
    }

    /**
     * Spawns a vertical water column for heavy-damage projectiles.
     *
     * <p>The column consists of two sub-layers:
     * <ol>
     *   <li>SPLASH drops with high vertical velocity and minimal horizontal spread.</li>
     *   <li>A large CustomCloud "pillar puff" that rises and lingers above the impact.</li>
     * </ol>
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnColumnLayer(level: ServerLevel, location: Vec3, power: Float) {
        val rng = level.random
        // Scale column height to how far above threshold we are
        val columnPower = ((power - COLUMN_THRESHOLD) / (POWER_MAX - COLUMN_THRESHOLD)).coerceIn(0f, 1f)
        val columnDrops = (2 + columnPower * 8).toInt().coerceIn(2, 10)

        // Tight upward SPLASH arc — narrow azimuth spread, high elevation
        repeat(columnDrops) {
            val azimuth = rng.nextDouble() * 2.0 * PI
            val lateralDrift = (0.02 + 0.06 * columnPower) * rng.nextDouble()  // nearly vertical
            val vy = 0.40 + 0.70 * columnPower + rng.nextDouble() * 0.20

            ParticleTool.sendParticle(
                level, ParticleTypes.SPLASH,
                location.x + rng.triangle(0.0, 0.03),
                location.y + 0.08,
                location.z + rng.triangle(0.0, 0.03),
                0,
                cos(azimuth) * lateralDrift,
                vy,
                sin(azimuth) * lateralDrift,
                1.0,
                true
            )
        }

        // Rising column puff — a large CustomCloud that floats upward
        val puffSize = 0.45f + 0.70f * columnPower
        val puffLife = (70 + 80 * columnPower).toInt()
        val columnPuff = CustomCloudOption(
            0.83f, 0.91f, 0.98f,
            puffLife,
            puffSize,
            -0.005f,          // negative gravity → drifts upward
            cooldown = false,
            light = false
        )

        ParticleTool.sendParticle(
            level, columnPuff,
            location.x + rng.triangle(0.0, 0.05),
            location.y + 0.12,
            location.z + rng.triangle(0.0, 0.05),
            0,
            rng.triangle(0.0, 0.012),
            0.12 + 0.18 * columnPower,   // upward drift
            rng.triangle(0.0, 0.012),
            1.0,
            true
        )
    }

    /**
     * Spawns expanding ring layers of CustomCloud particles around the impact point.
     *
     * <p>Each ring is evenly spaced around the circle, but individual particles receive
     * a small random position jitter so the ring looks organic rather than perfectly
     * geometric.  Inner rings are denser and faster; outer rings are sparser and slower,
     * replicating the natural physics of a ripple expanding outward.
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnRingLayers(level: ServerLevel, location: Vec3, power: Float) {
        val rng = level.random
        val ringLayers = (1 + power * 2).toInt().coerceIn(1, 3)
        val basePoints = (8 + power * 16).toInt().coerceIn(8, 24)

        for (layer in 0 until ringLayers) {
            // layerFactor: 1.0 for inner (fastest), decreasing toward outer
            val layerFactor = 1.0f - layer.toFloat() / ringLayers

            val speed = 0.06 + 0.18 * power * layerFactor
            val size = (0.10f + 0.42f * power) * (1.0f - layer * 0.20f)
            val lifetime = (80 + 100 * power * (1.0f + layer * 0.30f)).toInt()

            val ringParticle = CustomCloudOption(
                1f, 1f, 1f,
                lifetime,
                size,
                -0.002f,       // barely floats, sits on surface
                cooldown = false,
                light = false
            )

            val points = (basePoints - layer * 4).coerceAtLeast(6)
            val angleStep = 2.0 * PI / points

            repeat(points) { i ->
                val angle = angleStep * i
                // Per-particle speed jitter: ±15% variation so ring isn't perfectly uniform
                val speedJitter = speed * (0.85 + 0.30 * rng.nextDouble())
                val vx = cos(angle) * speedJitter
                val vz = sin(angle) * speedJitter

                // Position jitter — particles don't all spawn at dead-centre
                val posJitter = 0.015 * power
                ParticleTool.sendParticle(
                    level, ringParticle,
                    location.x + rng.triangle(0.0, posJitter),
                    location.y + 0.02,
                    location.z + rng.triangle(0.0, posJitter),
                    0,
                    vx, 0.0, vz,
                    1.0,
                    true
                )
            }
        }
    }

    /**
     * Spawns mist clouds of varied sizes above the impact surface.
     *
     * <p>Size distribution is intentionally skewed: most particles are small wisps
     * ({@code t²} bias), with occasional large billows.  RGB values are also
     * slightly randomised per-particle to break visual uniformity.
     *
     * @param level    server level.
     * @param location impact point.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun spawnVariedMist(level: ServerLevel, location: Vec3, power: Float) {
        if (power < MIST_THRESHOLD) return
        val rng = level.random
        val mistCount = (1 + power * 6).toInt().coerceIn(1, 7)

        repeat(mistCount) {
            // t² distribution → mostly small particles, rare large ones
            val t = rng.nextFloat().let { it * it }

            // Small wisps: 0.10–0.25  |  Mid billows: 0.25–0.55  |  Large: 0.55–0.90+
            val mistSize = ((0.10f + 0.80f * power) * (0.15f + 0.85f * (1f - t)))
                .coerceIn(0.08f, 1.10f)

            // Lifetime scales with size — big clouds linger longer
            val mistLife = (55 + (130 * power * (0.6f + 0.8f * mistSize / (0.10f + 0.80f * power))).toInt())
                .coerceAtLeast(30)

            // Per-particle colour variation: subtle blue-grey tint randomisation
            val r = 0.80f + 0.12f * rng.nextFloat()
            val g = 0.87f + 0.09f * rng.nextFloat()
            val b = 0.93f + 0.06f * rng.nextFloat()

            val mistParticle = CustomCloudOption(
                r, g, b,
                mistLife,
                mistSize,
                -0.003f,     // slow upward drift
                cooldown = false,
                light = false
            )

            // Spread spawn positions — bigger clouds spawn farther from centre
            val spreadRadius = 0.08 + 0.18 * power * (0.3 + 0.7 * mistSize)
            val angle = rng.nextDouble() * 2.0 * PI
            val radialOffset = spreadRadius * rng.nextDouble()

            ParticleTool.sendParticle(
                level, mistParticle,
                location.x + cos(angle) * radialOffset,
                location.y + 0.05 + rng.nextDouble() * 0.18 * power,
                location.z + sin(angle) * radialOffset,
                0,
                rng.triangle(0.0, 0.008 + 0.010 * power),
                0.006 + 0.025 * power * rng.nextDouble(),
                rng.triangle(0.0, 0.008 + 0.010 * power),
                1.0,
                true
            )
        }
    }

    //
    // Underwater bubble trail
    //

    /**
     * Spawns a bubble column trail along the projectile's underwater path.
     *
     * @param level      server level.
     * @param projectile projectile that entered the water.
     * @param location   entry point on the water surface.
     */
    private fun spawnUnderwaterBubbles(
        level: ServerLevel,
        projectile: Projectile,
        location: Vec3
    ) {
        val movementLength = projectile.deltaMovement.length()
        var i = 0.0
        while (i < movementLength) {
            val p = location.add(projectile.deltaMovement.normalize().scale(i))
            ParticleTool.sendParticle(
                level, ParticleTypes.BUBBLE_COLUMN_UP,
                p.x, p.y, p.z,
                1, 0.0, 0.0, 0.0, 0.001, false
            )
            i += 1.0
        }
    }

    //
    // Lava splash
    //

    /**
     * Spawns lava impact particles and sound.
     *
     * @param level    server level.
     * @param location impact point.
     * @param dir      outward splash direction.
     * @param state    block state of the lava block (used for BLOCK particle texture).
     */
    private fun spawnLavaSplash(
        level: ServerLevel,
        location: Vec3,
        dir: Vec3,
        state: BlockState
    ) {
        val rng = level.random
        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)

        for (i in 0..6) {
            val spreadDir = randomSpreadVec(rng, dir, 20.0)
            ParticleTool.sendParticle(
                level, particleData,
                location.x + 0.1 * i * dir.x,
                location.y + 0.1 * i * dir.y,
                location.z + 0.1 * i * dir.z,
                0,
                spreadDir.x, spreadDir.y, spreadDir.z,
                10.0,
                true
            )
        }

        ParticleTool.sendParticle(
            level, ParticleTypes.LAVA,
            location.x, location.y, location.z,
            4, 0.0, 0.0, 0.0, 0.6, true
        )

        level.playSound(
            null,
            BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
            SoundEvents.LAVA_POP,
            SoundSource.BLOCKS,
            1f, 1f
        )
    }

    //
    // Sound
    //

    /**
     * Plays the water hit sound scaled by [power].
     *
     * <p>Volume scales linearly with power; pitch is inversely proportional so
     * heavy impacts sound deeper and more resonant.
     *
     * @param level    server level.
     * @param location sound origin.
     * @param power    normalised damage power [0.1, 1.0].
     */
    private fun playWaterSound(level: ServerLevel, location: Vec3, power: Float) {
        level.playSound(
            null,
            BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
            ModSounds.HIT_WATER.get(),
            SoundSource.BLOCKS,
            0.25f + 0.75f * power,
            1.3f - 0.4f * power   // high power → lower pitch
        )
    }
}