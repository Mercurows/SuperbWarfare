package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.terrain.TerrainRevisionToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Vehicle terrain policy.
 *
 * The terrain itself is always a discrete Bullet triangle mesh for a tracked
 * vehicle. [BulletRamps] only controls whether a known vertical rise may be
 * replaced by a generated slope. Disabling it never falls back to raw block
 * AABB collision: every rise that is not a ramp stays a vertical mesh wall.
 */
@Serializable
data class TrackedTerrainDefinition(
    @SerialName("BulletRamps")
    var bulletRamps: BulletRampDefinition = BulletRampDefinition(),

    /**
     * Maximum horizontal reach before outward grid snapping. The compiled
     * envelope must fit; an oversized mechanism waits instead of being clipped.
     */
    @SerialName("WindowRadius")
    var windowRadiusBlocks: Int = 6,

    /** Blocks sampled above the chassis: enough for a deck or a low ceiling. */
    @SerialName("VerticalAbove")
    var verticalAboveBlocks: Int = 3,

    /** Blocks sampled below the chassis: enough for a pit or a lower floor. */
    @SerialName("VerticalBelow")
    var verticalBelowBlocks: Int = 4,
)

@Serializable
data class BulletRampDefinition(
    @SerialName("Enabled")
    var enabled: Boolean = false,

    /**
     * Maximum vertical rise of one generated ramp transition, metres.
     * Null means "not authored"; validator handles it only when enabled.
     */
    @SerialName("MaxClimb")
    var maxClimbM: Double? = null,

    /**
     * Horizontal length of the bottom edge of one generated ramp, metres.
     * Null means "not authored"; validator handles it only when enabled.
     */
    @SerialName("Step")
    var stepM: Double? = null,
)

/** Immutable, compiled form of [TrackedTerrainDefinition]. */
data class CompiledTerrain(
    val rampsEnabled: Boolean,
    val maxClimbM: Double,
    val stepM: Double,
    val windowRadiusBlocks: Int,
    val verticalAboveBlocks: Int,
    val verticalBelowBlocks: Int,
) {
    companion object {
        fun compile(definition: TrackedTerrainDefinition): CompiledTerrain {
            val ramps = definition.bulletRamps
            return CompiledTerrain(
                rampsEnabled = ramps.enabled,
                maxClimbM = ramps.maxClimbM ?: 1.0,
                stepM = ramps.stepM ?: 1.0,
                windowRadiusBlocks = definition.windowRadiusBlocks.coerceIn(3, 12),
                verticalAboveBlocks = definition.verticalAboveBlocks.coerceIn(2, 10),
                verticalBelowBlocks = definition.verticalBelowBlocks.coerceIn(2, 12),
            )
        }
    }
}

/**
 * One running-gear body, detached from Bullet, used only to refuse a ramp that
 * would be generated through that body.
 *
 * The probe is the body's centre and its world-aligned half extents. A wheel
 * resting on a slope is not a cut: the slope is near the bottom of the probe.
 * A slope through the inner band of the probe is an assist surface inserted
 * into the mechanism and must stay a wall instead.
 */
data class GearProbe(
    val x: Double,
    val y: Double,
    val z: Double,
    val halfX: Double,
    val halfY: Double,
    val halfZ: Double,
)

/** One detached Minecraft collision box in world coordinates. */
data class TerrainBox(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double,
) {
    fun intersects(other: TerrainBox): Boolean =
        minX < other.maxX && maxX > other.minX &&
            minY < other.maxY && maxY > other.minY &&
            minZ < other.maxZ && maxZ > other.minZ
}

/**
 * Conservative half-extents of the running gear in chassis-local coordinates.
 *
 * The sampling window must contain every wheel station and every belt run, or a
 * rebuild can install a mesh with the vehicle standing on nothing. The envelope
 * is derived from compiled geometry only, is never authored, and is deliberately
 * an over-estimate: a slightly larger window costs a few cached section reads, an
 * under-estimate costs the vehicle its support.
 */
data class TerrainEnvelope(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val minZ: Double,
    val maxZ: Double,
) {
    val halfX: Double get() = max(-minX, maxX)
    val halfY: Double get() = max(-minY, maxY)
    val halfZ: Double get() = max(-minZ, maxZ)

    companion object {
        fun of(spec: TrackedVehicleSpec): TerrainEnvelope {
            var minX = 0.0
            var maxX = 0.0
            var minY = 0.0
            var maxY = 0.0
            var minZ = 0.0
            var maxZ = 0.0

            fun include(center: Vec3, half: Vec3) {
                minX = minOf(minX, center.x - half.x)
                maxX = maxOf(maxX, center.x + half.x)
                minY = minOf(minY, center.y - half.y)
                maxY = maxOf(maxY, center.y + half.y)
                minZ = minOf(minZ, center.z - half.z)
                maxZ = maxOf(maxZ, center.z + half.z)
            }

            fun includeWheel(wheel: CompiledWheel) {
                // Cylinder axis is authored in chassis coordinates. Its support
                // radius on each world axis is radial radius times the length
                // of that axis projected onto the cylinder plane, plus the
                // axial half-width projection.
                val axis = wheel.axleDirection
                fun extent(component: Double): Double =
                    wheel.radius * sqrt((1.0 - component * component).coerceAtLeast(0.0)) +
                        wheel.halfWidth * abs(component)
                include(
                    wheel.assemblyCenter,
                    Vec3(extent(axis.x), extent(axis.y), extent(axis.z)),
                )
            }

            fun includeRun(run: CompiledRun) {
                val rotation = run.rotation.toQuaternion()
                val xAxis = rotation.transform(org.joml.Vector3f(1f, 0f, 0f))
                val yAxis = rotation.transform(org.joml.Vector3f(0f, 1f, 0f))
                val zAxis = rotation.transform(org.joml.Vector3f(0f, 0f, 1f))
                val half = run.halfExtents
                include(
                    run.center,
                    Vec3(
                        abs(xAxis.x) * half.x + abs(yAxis.x) * half.y + abs(zAxis.x) * half.z,
                        abs(xAxis.y) * half.x + abs(yAxis.y) * half.y + abs(zAxis.y) * half.z,
                        abs(xAxis.z) * half.x + abs(yAxis.z) * half.y + abs(zAxis.z) * half.z,
                    ),
                )
            }

            for (side in TrackedSide.entries) {
                val compiled = spec.side(side)
                for (wheel in compiled.wheels) {
                    includeWheel(wheel)
                }
                for (run in compiled.runs) {
                    includeRun(run)
                }
            }
            return TerrainEnvelope(minX, maxX, minY, maxY, minZ, maxZ)
        }
    }
}

/** One generated slope transition, retained for diagnostics and tests. */
data class TerrainRampPlan(
    val direction: RampDirection,
    val lowX: Double,
    val lowY: Double,
    val lowZ: Double,
    val highX: Double,
    val highY: Double,
    val highZ: Double,
    val lateralMin: Double,
    val lateralMax: Double,
    val riseM: Double,
    val runM: Double,
)

enum class RampDirection { EAST, WEST, SOUTH, NORTH }

/**
 * Which rises may become ramps for one mesh build.
 *
 * The corridor follows the vehicle's longitudinal axis, not the current
 * throttle. A rise whose climb is along the nose or the tail, and which sits
 * inside the track width, becomes a ramp. A rise beside the hull stays a
 * vertical wall. [forwardX] and [forwardZ] are already quantized: a few degrees
 * of yaw must not rebuild the mesh and drop every contact manifold.
 */
data class RampGate(
    val originX: Double,
    val originZ: Double,
    val forwardX: Double,
    val forwardZ: Double,
    val lateralHalfWidthM: Double,
    val minAlignment: Double = 0.5,
) {
    val headingBucket: Int get() = bucketOf(forwardX, forwardZ)

    /**
     * True when this rise climbs along the vehicle axis and is not beside the tracks.
     *
     * [face] is the world plane of the riser. [lateralCell] is the cell index
     * along that plane. Forward and backward climbs are both allowed; the sign
     * of the alignment distinguishes them from a side climb.
     */
    fun allows(direction: RampDirection, face: Double, lateralCell: Int): Boolean {
        val climbX = when (direction) {
            RampDirection.EAST -> 1.0
            RampDirection.WEST -> -1.0
            else -> 0.0
        }
        val climbZ = when (direction) {
            RampDirection.SOUTH -> 1.0
            RampDirection.NORTH -> -1.0
            else -> 0.0
        }
        val alignment = climbX * forwardX + climbZ * forwardZ
        // `<=` rejects the perpendicular world axis at a 30 degree heading,
        // where its alignment is exactly 0.5. The longitudinal axis is then
        // at least cos(15 deg) and still passes.
        if (abs(alignment) <= minAlignment) return false
        val faceX = if (climbX != 0.0) face else lateralCell + 0.5
        val faceZ = if (climbZ != 0.0) face else lateralCell + 0.5
        val offsetX = faceX - originX
        val offsetZ = faceZ - originZ
        val lateral = abs(offsetX * forwardZ - offsetZ * forwardX)
        return lateral <= lateralHalfWidthM
    }

    companion object {
        const val BUCKET_COUNT = 12

        fun bucketOf(forwardX: Double, forwardZ: Double): Int {
            if (hypot(forwardX, forwardZ) < 1.0e-8) return 3
            val step = Math.PI / 6.0
            var bucket = round(atan2(forwardZ, forwardX) / step).toInt()
            bucket %= BUCKET_COUNT
            if (bucket < 0) bucket += BUCKET_COUNT
            return bucket
        }

        fun forwardOf(bucket: Int): Pair<Double, Double> {
            val angle = bucket * Math.PI / 6.0
            return cos(angle) to sin(angle)
        }

        /** Snap heading to a 30 degree bucket so a small yaw reuses the mesh. */
        fun forVehicle(
            originX: Double,
            originZ: Double,
            forwardX: Double,
            forwardZ: Double,
            lateralHalfWidthM: Double,
        ): RampGate {
            val (fx, fz) = forwardOf(bucketOf(forwardX, forwardZ))
            return RampGate(originX, originZ, fx, fz, lateralHalfWidthM)
        }
    }
}

/**
 * Immutable mesh plus enough provenance to validate that the physics thread
 * installed exactly the geometry the sampler produced.
 *
 * [terrainRevisionHash] is the terrain cache revision the geometry was built
 * from; a plan whose token no longer matches the cache is stale and must be
 * discarded even if its triangles look plausible. [requestId] and
 * [runtimeGeneration] order plans from the same runtime so a slow build can
 * never overwrite a newer one.
 */
class TerrainMeshPlan(
    val originX: Double,
    val originY: Double,
    val originZ: Double,
    val vertices: FloatArray,
    val indices: IntArray,
    val ramps: List<TerrainRampPlan>,
    val wallQuadCount: Int,
    val triangleCount: Int,
    val contentHash: Long,
    val complete: Boolean = true,
    /** Region this plan covers, in world coordinates. */
    val region: TerrainBox = TerrainBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    /** Terrain-cache revision mixture the geometry came from. */
    val terrainRevisionHash: Long = Long.MIN_VALUE,
    /** Monotonic per-runtime request order. */
    val requestId: Long = -1L,
    /** Runtime generation that issued the request. */
    val runtimeGeneration: Long = -1L,
    /** Full section identity; null is allowed for standalone geometry tests only. */
    val terrainRevision: TerrainRevisionToken? = null,
    /**
     * Heading bucket the ramp gate used. `-1` means the plan was built without
     * a vehicle gate and may contain a ramp in every world direction.
     */
    val rampHeadingBucket: Int = -1,
    /**
     * One flag per triangle. True only for triangles added as a generated ramp,
     * using the same index order as [indices].
     */
    val rampTriangles: BooleanArray = BooleanArray(indices.size / 3),
) {
    val isEmpty: Boolean get() = indices.isEmpty()

    /** True when [other] supersedes this plan for the same runtime. */
    fun isSupersededBy(other: TerrainMeshPlan): Boolean =
        other.runtimeGeneration > runtimeGeneration ||
            (other.runtimeGeneration == runtimeGeneration && other.requestId > requestId)
}

/** Builder-facing result: either a complete mesh or the reason it cannot be used. */
sealed interface TrackedTerrainSampleResult {
    data class Ready(val plan: TerrainMeshPlan) : TrackedTerrainSampleResult
    data class Missing(val reason: String) : TrackedTerrainSampleResult
}
