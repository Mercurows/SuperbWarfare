package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowSpec
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * One road wheel's independent suspension, resolved for the runtime.
 *
 * The authoritative vehicle model is SuperbWarfare's own [TrackSuspensionDefinition]
 * and [TrackSuspensionOverride]: travel, frequency, damping ratios, force cap and
 * bump stop shape. MCSP only informs the behaviour (a sprung road wheel separate
 * from the drive and idler), never the schema. This type therefore keeps the
 * whole compiled [CompiledSuspension] rather than copying a subset of its fields,
 * so an audit can never drift from what the runtime will use.
 *
 * The row is bilateral. [SuspensionRowSpec] returns a signed force: positive in
 * compression, negative in droop. A one-way clamp is not an acceptable
 * simplification, because it would delete the restoring force at full droop.
 *
 * This is still pure data. It creates no Bullet object and can be produced on
 * any thread.
 */
data class CompiledSuspensionStation(
    val side: TrackedSide,
    val bone: String,
    /** Wheel centre at the bind pose, chassis frame, metres. */
    val wheelCenter: Vec3,
    /**
     * Chassis-side constraint pivot, chassis frame, metres.
     *
     * It equals [wheelCenter] so the two joint frames coincide at zero travel.
     * Putting the anchor a travel-length above the wheel would make the bind pose
     * sit at full compression and would tie the axle offset to the travel range.
     */
    val chassisPivot: Vec3,
    /** Wheel-side constraint pivot, wheel body frame, metres. Always the origin. */
    val wheelPivot: Vec3,
    /** Unit travel direction; `+Y` compresses. */
    val direction: Vec3,
    /** Unit axle direction. */
    val axle: Vec3,
    /** The complete compiled spring/damper row this station runs. */
    val suspension: CompiledSuspension,
) {
    val travelUpM: Double get() = suspension.travelUp
    val travelDownM: Double get() = suspension.travelDown
    val stiffnessNPerM: Double get() = suspension.stiffnessNPerM
    val compressionDampingNsPerM: Double get() = suspension.compressionDampingNsPerM
    val reboundDampingNsPerM: Double get() = suspension.reboundDampingNsPerM
    val maxForceN: Double get() = suspension.maxForceN
    val stationMassKg: Double get() = suspension.stationMassKg
    val staticSagM: Double get() = suspension.staticCompressionM
    val bumpStopStart: Double get() = suspension.bumpStopStart
    val bumpStopExponent: Double get() = suspension.bumpStopExponent

    /** Static load carried by this station, newtons. */
    val staticLoadN: Double get() = stationMassKg * GRAVITY

    /** Ratio of the force cap to the static load; must exceed 1 for a usable row. */
    val forceMargin: Double get() = if (staticLoadN > 0.0) maxForceN / staticLoadN else 0.0

    /** Undamped natural frequency implied by the compiled spring, Hz. */
    val naturalFrequencyHz: Double
        get() = if (stationMassKg > 0.0 && stiffnessNPerM > 0.0) {
            sqrt(stiffnessNPerM / stationMassKg) / (2.0 * Math.PI)
        } else {
            0.0
        }

    /** Damping ratio of the compiled compression rate. */
    val compressionDampingRatio: Double
        get() = criticalDampingNsPerM.let { if (it > 0.0) compressionDampingNsPerM / it else 0.0 }

    /** Damping ratio of the compiled rebound rate. */
    val reboundDampingRatio: Double
        get() = criticalDampingNsPerM.let { if (it > 0.0) reboundDampingNsPerM / it else 0.0 }

    val criticalDampingNsPerM: Double
        get() = if (stationMassKg > 0.0 && stiffnessNPerM > 0.0) {
            2.0 * sqrt(stiffnessNPerM * stationMassKg)
        } else {
            0.0
        }

    /** Where the progressive bump stop starts, metres of compression. */
    val bumpStopTravelM: Double get() = travelUpM * bumpStopStart.coerceIn(0.0, 0.99)

    /** True when the bind pose makes both joint frames coincide. */
    val pivotsCoincide: Boolean
        get() = chassisPivot.distanceToSqr(wheelCenter) < PIVOT_TOLERANCE_SQR &&
            wheelPivot.lengthSqr() < PIVOT_TOLERANCE_SQR

    /**
     * Adapt to the core's vehicle-agnostic row primitive.
     *
     * Every value comes from the compiled row, including the bump stop shape, so
     * changing `BumpStopStart` or `BumpStopExponent` in the vehicle JSON reaches
     * the force law instead of being replaced by a constant.
     */
    fun toRowSpec(): SuspensionRowSpec = SuspensionRowSpec(
        stationMassKg = stationMassKg.toFloat(),
        travelUpM = travelUpM.toFloat(),
        travelDownM = travelDownM.toFloat(),
        stiffnessNPerM = stiffnessNPerM.toFloat(),
        compressionDampingNsPerM = compressionDampingNsPerM.toFloat(),
        reboundDampingNsPerM = reboundDampingNsPerM.toFloat(),
        maxForceN = maxForceN.toFloat(),
        bumpStopStart = bumpStopStart.toFloat(),
        bumpStopExponent = bumpStopExponent.toFloat(),
    )

    companion object {
        /** Gravity the audit assumes, m/s². Must match the physics world. */
        const val GRAVITY = 9.81

        private const val PIVOT_TOLERANCE_SQR = 1.0e-12
    }
}

/**
 * Static audit of one side's independent suspension.
 *
 * B1's exit condition is that a single wheel's static load, travel and
 * compression/rebound behaviour are measurable and its limits are frozen. This
 * object records the numbers that decision needs; it deliberately does not
 * declare an acceptance threshold, which belongs in `ACCEPTANCE_LIMITS.md` and
 * requires the user's confirmation.
 */
data class SuspensionAudit(
    val side: TrackedSide,
    val stations: List<CompiledSuspensionStation>,
    /** Sum of the stations' sprung mass, kg. */
    val sprungMassKg: Double,
    /** Sum of the static loads, newtons. */
    val staticLoadTotalN: Double,
    /** Largest static sag across the stations, metres. */
    val maxStaticSagM: Double,
    /** Smallest force-cap margin across the stations. */
    val minForceMargin: Double,
    /** True when every station's static sag fits inside its compression travel. */
    val staticSagFitsTravel: Boolean,
    /** True when every station's static sag stays off its bump stop. */
    val staticSagClearsBumpStop: Boolean,
    /** Axles that are not parallel to the side's lateral axis. */
    val misalignedAxles: List<String>,
    /** Stations whose travel direction is not vertical. */
    val cantedDirections: List<String>,
    /** Stations whose two joint frames do not coincide at the bind pose. */
    val misalignedPivots: List<String>,
) {
    val stationCount: Int get() = stations.size

    /** True when every row produces a restoring force in droop. */
    val bilateralRecovery: Boolean
        get() = stations.isNotEmpty() &&
            stations.all { station -> PhysicsSuspensionRow.droopRestoringForceN(station.toRowSpec()) < 0f }
}

object TrackedSuspensionCompiler {

    /**
     * Resolve every suspended station of [side] into a runtime row.
     *
     * Only `Support` wheels are sprung. A drive sprocket and an idler are
     * constrained by the belt and the tensioner, not by a spring, so giving them
     * a spring row would double-count the load path.
     */
    fun compile(spec: TrackedVehicleSpec, side: TrackedSide): SuspensionAudit =
        compile(spec.side(side).wheels, side)

    /** Resolve one already-compiled wheel set; used by the compiler and by tests. */
    fun compile(wheels: List<CompiledWheel>, side: TrackedSide): SuspensionAudit {
        val lateral = lateralAxis(side)
        val stations = wheels
            .filter { it.isSuspended }
            .map { wheel -> station(side, wheel, lateral) }

        val sprung = stations.sumOf { it.stationMassKg }
        val staticLoad = stations.sumOf { it.staticLoadN }
        val maxSag = stations.maxOfOrNull { it.staticSagM } ?: 0.0
        val minMargin = stations.minOfOrNull { it.forceMargin } ?: 0.0
        val sagFits = stations.all { it.staticSagM <= it.travelUpM }
        // The bump stop knee is per-station data, not a shared constant: an
        // override that moves it must move this check too.
        val sagClears = stations.all { station -> station.staticSagM < station.bumpStopTravelM }
        return SuspensionAudit(
            side = side,
            stations = stations,
            sprungMassKg = sprung,
            staticLoadTotalN = staticLoad,
            maxStaticSagM = maxSag,
            minForceMargin = minMargin,
            staticSagFitsTravel = sagFits,
            staticSagClearsBumpStop = sagClears,
            misalignedAxles = stations
                .filter { abs(abs(it.axle.dot(lateral)) - 1.0) > AXIS_TOLERANCE }
                .map { it.bone },
            cantedDirections = stations
                .filter { abs(it.direction.y - 1.0) > AXIS_TOLERANCE }
                .map { it.bone },
            misalignedPivots = stations.filterNot { it.pivotsCoincide }.map { it.bone },
        )
    }

    /**
     * Chassis-side pivot of a station.
     *
     * The pivot is the authored wheel centre. Travel is a free DOF of the joint,
     * not something the anchor has to manufacture: offsetting the anchor by
     * `travelUp` would place the bind pose at full compression and couple the
     * axle offset to the travel range.
     */
    fun anchorFor(wheel: CompiledWheel, @Suppress("UNUSED_PARAMETER") direction: Vec3): Vec3 =
        wheel.bindCenter

    private fun station(
        side: TrackedSide,
        wheel: CompiledWheel,
        lateral: Vec3,
    ): CompiledSuspensionStation {
        val suspension = wheel.suspension
        val pivot = anchorFor(wheel, suspension.direction)
        return CompiledSuspensionStation(
            side = side,
            bone = wheel.bone,
            wheelCenter = wheel.bindCenter,
            chassisPivot = pivot,
            wheelPivot = Vec3.ZERO,
            direction = suspension.direction,
            axle = wheel.axleDirection,
            suspension = suspension,
        )
    }

    /**
     * Lateral axis for a side, as a direction.
     *
     * Both sides use `+X`: an axle direction is not a signed position, and the
     * compiled wheels already carry `+X`. This exists so a future canted layout
     * has one place to change instead of two.
     */
    fun lateralAxis(@Suppress("UNUSED_PARAMETER") side: TrackedSide): Vec3 = Vec3(1.0, 0.0, 0.0)

    private const val AXIS_TOLERANCE = 1.0e-6
}
