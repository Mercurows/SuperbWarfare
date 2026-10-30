package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec
import cn.solarmoon.spark_core.physics.track.TrackRunSpec

/**
 * Turns one audited [CompiledTrackSide] into generic core `TrackRunSpec`s.
 *
 * This is pure data: it creates no Bullet object and runs on any thread.
 *
 * [compileRuns] is the current topology: one single-link run per adjacent wheel
 * pair, with the two end constraints owned by the caller. A run has no internal
 * joint because its two ends are constrained to wheel bodies, which is why the
 * specs have exactly one link each and an empty joint list.
 *
 * The external constraints are **not** in these specs: `TrackRunSpec` must stay
 * vehicle-agnostic and has no wheel concept. The caller reads
 * `CompiledRun.first/second` and creates the revolute and the single-normal
 * joint against the wheel bodies it owns.
 *
 * The legacy 79-shoe adapter lives in
 * [com.atsuishio.superbwarfare.physics.tracked.legacy.LegacyShoeChainRunSpecs].
 */
object TrackedRunSpecs {

    /**
     * Build one single-link `TrackRunSpec` per adjacent wheel pair of [side].
     *
     * The adapter never re-derives geometry: position, rotation, extents and mass
     * come straight from the compiled run, so a run that failed the audit cannot
     * be assembled into a different, apparently valid shape here.
     */
    fun compileRuns(spec: TrackedVehicleSpec, side: TrackedSide): List<TrackRunSpec> =
        compileRuns(spec.side(side), spec.vehicleId, spec.drivetrain.friction)

    /**
     * Compile one already-built side.
     *
     * This overload exists so a diagnostic fixture can use the **same** adapter
     * as production instead of constructing `TrackRunSpec` by hand. The side is
     * still the complete input: position, rotation, extents and mass are read
     * from its runs, never from the caller.
     */
    fun compileRuns(
        compiled: CompiledTrackSide,
        vehicleId: String,
        friction: Double,
    ): List<TrackRunSpec> {
        val side = compiled.side
        require(compiled.runs.isNotEmpty()) { "$side has no compiled belt runs" }
        return compiled.runs.map { run ->
            val half = run.halfExtents
            require(half.x > 0.0 && half.y > 0.0 && half.z > 0.0) {
                "$side run ${run.index} has a degenerate extent: $half"
            }
            require(run.massKg > 0.0) { "$side run ${run.index} has no mass" }
            TrackRunSpec(
                name = "${vehicleId}_track_${side.name.lowercase()}_run_${run.index}",
                links = listOf(
                    TrackRunLinkSpec(
                        massKg = run.massKg.toFloat(),
                        shape = TrackRunLinkShape.Box(half),
                        position = run.center,
                        rotationX = run.rotation.x,
                        rotationY = run.rotation.y,
                        rotationZ = run.rotation.z,
                        rotationW = run.rotation.w,
                    ),
                ),
                joints = emptyList(),
                collisionGroup = cn.solarmoon.spark_core.physics.body.CollisionGroups.TRACK_LINK,
                // A run may touch its own wheels/chassis and the per-vehicle
                // local terrain mesh. It must never touch raw block AABB
                // terrain.
                collideWithGroups = TrackedTerrainCollision.TERRAIN_MESH or
                    cn.solarmoon.spark_core.physics.body.CollisionGroups.WHEEL or
                    cn.solarmoon.spark_core.physics.body.CollisionGroups.CHASSIS,
                collideWithOwnerGroups = cn.solarmoon.spark_core.physics.body.CollisionGroups.WHEEL or
                    cn.solarmoon.spark_core.physics.body.CollisionGroups.CHASSIS or
                    TrackedTerrainCollision.TERRAIN_MESH,
                friction = friction.toFloat(),
                explicitGravity = null,
            )
        }
    }

    /**
     * Total mass of every run on [side] as the core adapter sees it, kg.
     *
     * This is the `Float` total, which can differ from the compiled `Double`
     * budget by a few micrograms. Callers that need the authored budget must use
     * [CompiledTrackSide.runMassTotalKg]; this value exists to prove the adapter
     * did not drop or duplicate a run.
     */
    fun runMassTotalKg(spec: TrackedVehicleSpec, side: TrackedSide): Double =
        compileRuns(spec, side).sumOf { it.links.first().massKg.toDouble() }
}
