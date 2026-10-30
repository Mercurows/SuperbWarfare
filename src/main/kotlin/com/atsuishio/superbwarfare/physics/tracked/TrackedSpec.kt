package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.SuspensionRowSpec
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.util.Collections
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * An immutable rotation written as four scalars.
 *
 * Compiled results cross a thread boundary and are cached, so they must not
 * carry a mutable JOML [Quaternionf] that a caller could change afterwards.
 */
data class CompiledRotation(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f,
) {
    val isIdentity: Boolean get() = x == 0f && y == 0f && z == 0f && w == 1f

    /** A defensive copy; mutating the result never changes this value. */
    fun toQuaternion(): Quaternionf = Quaternionf(x, y, z, w)

    companion object {
        @JvmField
        val IDENTITY = CompiledRotation()

        /**
         * Build a rotation whose local `X`, `Y` and `Z` axes are [xAxis], [yAxis]
         * and [zAxis], given in the parent frame.
         *
         * The axes must be orthonormal; a constraint frame with a skewed basis
         * would silently change which direction a joint locks.
         */
        fun fromBasis(xAxis: Vec3, yAxis: Vec3, zAxis: Vec3): CompiledRotation {
            val x = xAxis.normalize()
            val y = yAxis.normalize()
            val z = zAxis.normalize()
            require(abs(x.dot(y)) < 1.0e-6 && abs(x.dot(z)) < 1.0e-6 && abs(y.dot(z)) < 1.0e-6) {
                "basis is not orthogonal: $x / $y / $z"
            }
            // Column-major rotation matrix -> quaternion (Shepperd's method).
            val m00 = x.x; val m01 = y.x; val m02 = z.x
            val m10 = x.y; val m11 = y.y; val m12 = z.y
            val m20 = x.z; val m21 = y.z; val m22 = z.z
            val trace = m00 + m11 + m22
            return if (trace > 0.0) {
                val s = sqrt(trace + 1.0) * 2.0
                CompiledRotation(
                    x = ((m21 - m12) / s).toFloat(),
                    y = ((m02 - m20) / s).toFloat(),
                    z = ((m10 - m01) / s).toFloat(),
                    w = (0.25 * s).toFloat(),
                )
            } else if (m00 > m11 && m00 > m22) {
                val s = sqrt(1.0 + m00 - m11 - m22) * 2.0
                CompiledRotation(
                    x = (0.25 * s).toFloat(),
                    y = ((m01 + m10) / s).toFloat(),
                    z = ((m02 + m20) / s).toFloat(),
                    w = ((m21 - m12) / s).toFloat(),
                )
            } else if (m11 > m22) {
                val s = sqrt(1.0 + m11 - m00 - m22) * 2.0
                CompiledRotation(
                    x = ((m01 + m10) / s).toFloat(),
                    y = (0.25 * s).toFloat(),
                    z = ((m12 + m21) / s).toFloat(),
                    w = ((m02 - m20) / s).toFloat(),
                )
            } else {
                val s = sqrt(1.0 + m22 - m00 - m11) * 2.0
                CompiledRotation(
                    x = ((m02 + m20) / s).toFloat(),
                    y = ((m12 + m21) / s).toFloat(),
                    z = (0.25 * s).toFloat(),
                    w = ((m10 - m01) / s).toFloat(),
                )
            }
        }
    }
}

/** One compiled suspension row. All values are SI, already scaled to the wheel's mass share. */
data class CompiledSuspension(
    val travelUp: Double,
    val travelDown: Double,
    /** Undamped natural frequency, Hz. Kept for audits only. */
    val frequency: Double,
    val stiffnessNPerM: Double,
    val compressionDampingNsPerM: Double,
    val reboundDampingNsPerM: Double,
    val maxForceN: Double,
    val bumpStopStart: Double,
    val bumpStopExponent: Double,
    /** Unit travel direction in the chassis frame; `+Y` is compression for the current solver. */
    val direction: Vec3,
    /** Sprung mass this row carries, kg. */
    val stationMassKg: Double,
    /** Static sag at the authored wheel position, `g / omega^2`, metres. */
    val staticCompressionM: Double,
) {
    val isSprung: Boolean get() = stationMassKg > 0.0
}

data class CompiledTensioner(
    val travel: Double,
    val preloadN: Double,
    val dampingNsPerM: Double,
    /** Unit free-travel axis in the chassis frame. */
    val axis: Vec3,
)

data class CompiledWheel(
    val side: TrackedSide,
    val role: TrackWheelRole,
    val bone: String,
    /** Stable ordinal inside its side, in longitudinal order. */
    val index: Int,
    /**
     * Authored bind position in the chassis frame, metres: the model pivot with
     * [TrackedVehicleDefinition.chassisOrigin] subtracted. This is the value the
     * vehicle resource is audited against.
     */
    val bindCenter: Vec3,
    /**
     * Position the wheel body is created at. It equals [bindCenter]; a tensioned
     * idler's free travel is a joint coordinate, not a new bind position.
     */
    val assemblyCenter: Vec3,
    val radius: Double,
    /** Lateral half width, metres. */
    val halfWidth: Double,
    /** Unit axle direction in the chassis frame; `+X` is the lateral axis. */
    val axleDirection: Vec3,
    /** Mass of this wheel body, kg. */
    val massKg: Double,
    val suspension: CompiledSuspension,
    val tensioner: CompiledTensioner? = null,
    /** Which side of this wheel the belt wraps around. */
    val wrap: TrackWrap = TrackWrap.OUTSIDE,
) {
    val isSuspended: Boolean get() = role == TrackWheelRole.SUPPORT
    val receivesDrive: Boolean get() = role == TrackWheelRole.DRIVE_GUIDE
}

/** One cube of the single-link template, expressed relative to the pin axis. */
data class CompiledLinkCube(
    val center: Vec3,
    val half: Vec3,
    val rotation: CompiledRotation = CompiledRotation.IDENTITY,
)

/**
 * The physical shoe: a single oriented box in the link's pin frame.
 *
 * [boxCenterInPinFrame] is not assumed to be zero. A template whose pin sits on
 * the inner wearing surface has the box hanging outward, and the rigid body
 * origin must be offset accordingly or the shoe would straddle the pin.
 */
data class CompiledPhysicalLinkShape(
    val halfExtents: Vec3,
    val boxCenterInPinFrame: Vec3,
)

data class CompiledLinkTemplate(
    val bone: String,
    val cubes: List<CompiledLinkCube>,
    val width: Double,
    val thickness: Double,
    val length: Double,
    /**
     * Lateral centre of the shoe in the vehicle-local metre frame.
     *
     * The template bone's own pivot is authoritative here: a belt is not
     * necessarily centred on the road-wheel hubs, and averaging wheel centres
     * would silently shift the whole physical run sideways.
     */
    val centerX: Double = 0.0,
    /** AABB of the template cubes in the pin frame; the physical shoe. */
    val physical: CompiledPhysicalLinkShape = CompiledPhysicalLinkShape(
        Vec3(width * 0.5, thickness * 0.5, length * 0.5),
        Vec3.ZERO,
    ),
)

/**
 * One link of the assembled closed loop, in the chassis frame.
 *
 * This is the single source of truth for link pose: the static geometry audit
 * and the `TrackRunSpec` adapter both consume it, so they cannot drift apart.
 */
data class CompiledLinkPlacement(
    val index: Int,
    /** Leading pin (the hinge shared with the previous link), chassis frame. */
    val pinStart: Vec3,
    /** Trailing pin (the hinge shared with the next link), chassis frame. */
    val pinEnd: Vec3,
    /** Rigid-body origin: pin-frame midpoint plus the rotated box offset. */
    val center: Vec3,
    val rotation: CompiledRotation,
    /** Shoe box centre in this link's pin frame. */
    val boxCenterInPinFrame: Vec3,
    val halfExtents: Vec3,
    val chordLength: Double,
)

/**
 * Static geometry audit of one side, in metres.
 *
 * [minPinChordClearanceM] is the pin-polygon metric that older audits reported;
 * it is *not* a shoe clearance. [minShoeWheelGapM] is the physical box-to-wheel
 * distance and is the value that decides whether the belt can be assembled
 * without overlapping collision shapes.
 */
data class CompiledGeometryAudit(
    val minPinChordClearanceM: Double,
    val minShoeWheelGapM: Double,
    val worstLinkIndex: Int,
    val worstWheelBone: String,
    /** Lateral overlap between the worst shoe/wheel pair, metres. */
    val worstLateralOverlapM: Double,
    val checkedPairs: Int,
) {
    val interpenetrates: Boolean get() = minShoeWheelGapM < -INTERPENETRATION_TOLERANCE_M

    companion object {
        /**
         * Audit threshold, not a vehicle acceptance limit: a shoe may touch its
         * wheel, but overlapping collision shapes by more than a millimetre is a
         * modelling defect that would resolve as a jolt at assembly.
         */
        const val INTERPENETRATION_TOLERANCE_M = 0.001
    }
}

/** Which end of a run an attachment describes. */
enum class RunEnd { FIRST, SECOND }

/**
 * How one wheel body is mounted to the chassis.
 *
 * This is the SBW-side decision that MCSP expresses with its own
 * `carrier`/`hub`/`knuckle`/`spin` hierarchy. SBW keeps one dynamic wheel body
 * per belt member and states only the constraint it needs:
 *
 * - `PRISMATIC`: one translational degree of freedom along [axis], limited to
 *   `[-limitDownM, +limitUpM]`, all rotation locked. Used by sprung road wheels
 *   and by the tensioned idler.
 * - `FIXED`: no degree of freedom. Used by the drive sprocket and by a return
 *   roller.
 *
 * No mount permits independent wheel-face spin: the belt carries the rolling
 * contact, and the visible wheel rotation is render-only.
 */
enum class WheelMountKind { PRISMATIC, FIXED }

/**
 * One wheel body's mounting constraint, resolved from its authored role.
 *
 * The constraint is a full [CompiledJointPlan], so a runtime never has to infer
 * a frame or a degree of freedom from the role name. The role only decides which
 * plan the compiler builds.
 *
 * [springRow] is present only for a sprung `Support` wheel; [preloadN] and
 * [dampingNsPerM] are present only for a tensioned `IdlerGuide`. They are kept
 * as separate fields rather than a nullable union so an audit cannot read a
 * tensioner preload as a spring rate.
 */
data class CompiledWheelMount(
    val wheelIndex: Int,
    val bone: String,
    val kind: WheelMountKind,
    /** Unit travel direction in the chassis frame; zero for a fixed mount. */
    val axis: Vec3,
    val limitUpM: Double,
    val limitDownM: Double,
    /** Present for a sprung road wheel. */
    val springRow: SuspensionRowSpec? = null,
    /** Present for a tensioned idler, newtons. */
    val preloadN: Double? = null,
    /** Present for a tensioned idler, N·s/m. */
    val dampingNsPerM: Double? = null,
    /** Chassis-side frame and full six-axis state of the mount. */
    val joint: CompiledJointPlan? = null,
) {
    val isSprung: Boolean get() = springRow != null
    val isTensioned: Boolean get() = preloadN != null

    /** True when this mount locks every rotation, as every belt-member mount must. */
    val locksAllRotation: Boolean
        get() = joint == null || joint.angular.all { it is CompiledAxisLimit.Locked }
}

/**
 * One end of a run, expressed in both bodies' local frames.
 *
 * The first end becomes a revolute about the wheel axle and the second end locks
 * only the translation along the run's normal; every other degree of freedom is
 * free. Anchors are in body-local metres, so a runtime can create the joint
 * without re-deriving any geometry.
 *
 * [joint] is the authoritative description of the end: [anchorInRun],
 * [anchorInWheel], [frameInRun] and [frameInWheel] are the same data in the
 * layout the legacy audit already used, kept so both views cannot drift.
 */
data class CompiledRunAttachment(
    val end: RunEnd,
    val wheelIndex: Int,
    /** Anchor in the wheel body frame, metres. The wheel origin is its centre. */
    val anchorInWheel: Vec3,
    /** The same point in the run body frame, metres. */
    val anchorInRun: Vec3,
    /** Constraint frame on the wheel body. */
    val frameInWheel: CompiledRotation,
    /** Constraint frame on the run body. */
    val frameInRun: CompiledRotation,
    /**
     * Run-local unit normal held at the second end. Null at the first end, which
     * locks all translation and frees one rotation instead.
     */
    val lockedNormalLocal: Vec3? = null,
    /** Full six-axis description of this end. */
    val joint: CompiledJointPlan,
) {
    val isFirst: Boolean get() = end == RunEnd.FIRST
}

/**
 * One physical belt run: the box that spans a single adjacent wheel pair.
 *
 * [rotation] is about the lateral axis only, so the box's local `+Z` follows
 * the run direction. [halfExtents] is `(belt width/2, depth/2, half length)`;
 * the belt is wider than the wheels, so this is not the wheel width.
 */
data class CompiledRun(
    val index: Int,
    val firstWheelIndex: Int,
    val secondWheelIndex: Int,
    /** True when this pair closes the loop (last wheel back to the first). */
    val closesLoop: Boolean,
    /** Box centre in the chassis frame, metres. */
    val center: Vec3,
    val rotation: CompiledRotation,
    val halfExtents: Vec3,
    val massKg: Double,
    val first: CompiledRunAttachment,
    val second: CompiledRunAttachment,
) {
    /** Full length along the run, metres. */
    val lengthM: Double get() = halfExtents.z * 2.0

    /** Both ends, in order. */
    val attachments: List<CompiledRunAttachment> get() = listOf(first, second)
}

/**
 * Legacy shoe-chain audit of one side.
 *
 * The 79-shoe closed chain is no longer the physical topology. Its results are
 * kept so the historical S1 and B2.0 evidence stays reproducible, and so an
 * existing audit can be re-read, but nothing on the run path may consume them.
 */
data class LegacyShoeChainAudit(
    val linkTemplate: CompiledLinkTemplate,
    val loop: ClosedTrackLoopSolver.Result,
    val driveIndex: Int,
    val idlerIndex: Int,
    val linkMassKg: Double,
    val placements: List<CompiledLinkPlacement>,
    val geometry: CompiledGeometryAudit,
) {
    val linkMassTotalKg: Double get() = linkMassKg * loop.linkCount
}

/** One side's compiled mechanism. Immutable; every list is defensively copied. */
class CompiledTrackSide(
    val side: TrackedSide,
    wheels: List<CompiledWheel>,
    /** Belt geometry of the run topology. */
    val belt: CompiledBeltGeometry,
    /**
     * Belt traversal order as wheel indices. Adjacent entries, including the
     * last back to the first, each own one run.
     */
    beltOrder: List<Int>,
    members: List<CompiledBeltMember>,
    runs: List<CompiledRun>,
    mounts: List<CompiledWheelMount>,
    /** Independent suspension rows for the road wheels of this side. */
    val suspension: SuspensionAudit,
    /** Historical 79-shoe audit; present only when that chain also compiled. */
    val legacy: LegacyShoeChainAudit? = null,
) {
    val wheels: List<CompiledWheel> = wheels.toList()
    val beltOrder: List<Int> = beltOrder.toList()
    val members: List<CompiledBeltMember> = members.toList()
    val runs: List<CompiledRun> = runs.toList()
    val mounts: List<CompiledWheelMount> = mounts.toList()

    val wheelMassTotalKg: Double get() = wheels.sumOf { it.massKg }
    val runMassTotalKg: Double get() = runs.sumOf { it.massKg }

    /**
     * Running-gear mass of this side.
     *
     * Option A of the mass plan: the run budget equals the old shoe budget, so
     * this stays exactly what it was before the topology change and the vehicle
     * total does not move. Wheels keep their authored mass.
     */
    val runningGearMassKg: Double get() = runMassTotalKg + wheelMassTotalKg

    /** Ordered wheels of the belt, cyclic. */
    val beltWheels: List<CompiledWheel> get() = beltOrder.map { wheels[it] }

    fun mount(wheelIndex: Int): CompiledWheelMount =
        mounts.firstOrNull { it.wheelIndex == wheelIndex }
            ?: throw TrackedGeometryException("${side.name} wheel $wheelIndex has no mount")

    fun run(index: Int): CompiledRun =
        runs.firstOrNull { it.index == index }
            ?: throw TrackedGeometryException("${side.name} has no run $index")

    /** The legacy shoe audit, or a clear failure if this side has none. */
    fun requireLegacy(): LegacyShoeChainAudit = legacy
        ?: throw TrackedGeometryException("${side.name} has no legacy shoe-chain audit")

    @Deprecated("Legacy shoe chain; use the run topology")
    val loop: ClosedTrackLoopSolver.Result get() = requireLegacy().loop

    @Deprecated("Legacy shoe chain; use the run topology")
    val placements: List<CompiledLinkPlacement> get() = requireLegacy().placements

    @Deprecated("Legacy shoe chain; use the run topology")
    val linkMassKg: Double get() = requireLegacy().linkMassKg

    @Deprecated("Legacy shoe chain; use the run topology")
    val linkTemplate: CompiledLinkTemplate get() = requireLegacy().linkTemplate

    @Deprecated("Legacy shoe chain; use the run topology")
    val geometry: CompiledGeometryAudit get() = requireLegacy().geometry

    @Deprecated("Legacy shoe chain; use the run topology")
    val driveIndex: Int get() = requireLegacy().driveIndex

    @Deprecated("Legacy shoe chain; use the run topology")
    val idlerIndex: Int get() = requireLegacy().idlerIndex
}

/** Severity of one audit finding. `ERROR` blocks compilation; `WARN` needs review. */
enum class AuditSeverity { ERROR, WARN, INFO }

data class TrackedAuditFinding(
    val severity: AuditSeverity,
    val code: String,
    val message: String,
)

/** Immutable copy of the authored drivetrain block; no live data-class reference escapes. */
data class CompiledDrivetrain(
    val maxDriveTorqueNm: Double,
    val maxBrakeTorqueNm: Double,
    val maxSpeedMps: Double,
    val differential: Double,
    val rollingResistance: Double,
    val friction: Double,
    /**
     * MCSP-style control-response bounds, m/s². Null means the controller keeps
     * its immediate target rather than inventing a value from the MCSP sample.
     */
    val accelMps2: Double? = null,
    val brakeMps2: Double? = null,
    /** Opposing-side speed at full steer, m/s. Null keeps [differential]. */
    val pivotSpeedMps: Double? = null,
    /** Seconds for the full steering lever travel. Null keeps it instantaneous. */
    val steerTimeSeconds: Double? = null,
    /** Feed measured traction into a slipping side's contact target (MCSP intent). */
    val tractionLimit: Boolean = false,
) {
    /** True when at least one MCSP-style response bound is authored. */
    val hasResponse: Boolean
        get() = accelMps2 != null || brakeMps2 != null || pivotSpeedMps != null || steerTimeSeconds != null
}

enum class MassBasis { BOX, CYLINDER }

/** Inertia of one rigid body about its own centre of mass, in its local frame. */
data class CompiledBodyMass(
    val name: String,
    val massKg: Double,
    val inertiaKgM2: Vec3,
    val basis: MassBasis,
)

/**
 * Mass and inertia audit for one vehicle.
 *
 * Hull inertia is deliberately absent: it needs the vehicle's collision OBB,
 * which is not part of the mechanism definition. Inventing a hull inertia from
 * the running gear would be worse than recording that it is unresolved.
 */
data class TrackedMassAudit(
    /**
     * Mass left after every running-gear body. This is the sprung assembly the
     * springs carry, **not** a final chassis body mass: turret, barrel and hull
     * inertia still have to be resolved from the vehicle's own mass plan.
     */
    val sprungAssemblyMassKg: Double,
    val runningGearMassKg: Double,
    val totalMassKg: Double,
    /** One representative shoe per side. Legacy topology only. */
    val shoes: List<CompiledBodyMass>,
    val wheels: List<CompiledBodyMass>,
    /** One representative belt run per side; current topology. */
    val runs: List<CompiledBodyMass> = emptyList(),
) {
    /** True when every declared body mass sums to the authored total. */
    fun closes(): Boolean = abs(sprungAssemblyMassKg + runningGearMassKg - totalMassKg) <= 1.0e-6

    /** Running-gear detail must add up to the reported running-gear total. */
    fun detailCloses(): Boolean {
        val wheelTotal = wheels.sumOf { it.massKg }
        val runTotal = runs.sumOf { it.massKg }
        return abs(wheelTotal + runTotal - runningGearMassKg) <= 1.0e-6
    }
}

/**
 * Visible belt description, kept separate from the physical run.
 *
 * [visiblePitchM] is null unless a real source exists. The 99A track bones all
 * share one pivot, so the model contains no spacing information: deriving a
 * visible pitch by dividing the belt length by the bone count would be a
 * fabricated number.
 */
data class TrackedRenderTemplate(
    val side: TrackedSide,
    val linkTemplateBone: String,
    val visibleBones: List<String>,
    val visiblePitchM: Double?,
    /** Belt thickness used by the visible centre line, metres. */
    val thicknessM: Double = 0.0,
    /** Fixed plate count from the rest envelope. Zero when no pitch source exists. */
    val visibleCount: Int = 0,
    /** Rest envelope length divided by [visibleCount], metres. */
    val restPitchM: Double = 0.0,
    /** Link-count phase that puts plate zero on the template pivot. */
    val initialPhase: Double = 0.0,
    val seatAlongM: Double = 0.0,
    val seatNormalM: Double = 0.0,
    val seatAngleRad: Double = 0.0,
)

/**
 * Immutable result of compiling one vehicle definition against one model.
 * It holds no Minecraft or Bullet object and can be produced on any thread.
 */
class TrackedVehicleSpec(
    val vehicleId: String,
    val model: String,
    val modelSha256: String,
    val renderScale: Double,
    /** Model point, metres, that the chassis body origin corresponds to. */
    val chassisOriginX: Double,
    val chassisOriginY: Double,
    val chassisOriginZ: Double,
    val massKg: Double,
    val pitch: Double,
    val pinInset: Double,
    /** Authored minimum static shoe gap, metres. */
    val clearanceM: Double,
    val linkMassKg: Double,
    val angularLimitDegrees: Double,
    val drivetrain: CompiledDrivetrain,
    /** Immutable local mesh/ramp policy; only generated geometry reaches Bullet. */
    val terrain: CompiledTerrain,
    sides: Map<TrackedSide, CompiledTrackSide>,
    audit: List<TrackedAuditFinding>,
    renders: Map<TrackedSide, TrackedRenderTemplate>,
    val mass: TrackedMassAudit,
) {
    val sides: Map<TrackedSide, CompiledTrackSide> = Collections.unmodifiableMap(LinkedHashMap(sides))
    val audit: List<TrackedAuditFinding> = audit.toList()
    val renders: Map<TrackedSide, TrackedRenderTemplate> = Collections.unmodifiableMap(LinkedHashMap(renders))

    fun side(side: TrackedSide): CompiledTrackSide =
        sides[side] ?: throw TrackedGeometryException("side $side was not compiled")

    /** Total number of physically simulated runs, both sides. */
    val totalRunCount: Int get() = sides.values.sumOf { it.runs.size }

    /** Total number of dynamic wheels, both sides. */
    val totalWheelCount: Int get() = sides.values.sumOf { it.wheels.size }

    /** Total number of legacy shoes, both sides; only valid when the chain compiled. */
    @Deprecated("Legacy shoe chain; the physical topology is the run list")
    val totalLinkCount: Int get() = sides.values.sumOf { it.requireLegacy().loop.linkCount }

    /** Sum of every sprung station mass; must stay below [massKg]. */
    val stationMassTotal: Double get() = sides.values.sumOf { side ->
        side.wheels.filter { it.isSuspended }.sumOf { it.suspension.stationMassKg }
    }

    /** Every run plus every wheel body, both sides. */
    val runningGearMassKg: Double get() = sides.values.sumOf { it.runningGearMassKg }

    /** Remaining sprung assembly carried by the springs; not the final chassis body mass. */
    val sprungAssemblyMassKg: Double
        get() = (massKg - runningGearMassKg).coerceAtLeast(0.0)
}

/**
 * Outcome of one compilation.
 *
 * [spec] is null whenever any error was reported, so a mechanically invalid
 * definition can never be adapted into a runtime specification. [findings] is
 * always available for reporting.
 */
class TrackedCompilation(
    findings: List<TrackedAuditFinding>,
    val spec: TrackedVehicleSpec?,
) {
    val findings: List<TrackedAuditFinding> = findings.toList()

    val hasErrors: Boolean get() = findings.any { it.severity == AuditSeverity.ERROR }

    fun requireSpec(): TrackedVehicleSpec {
        spec?.let { return it }
        val errors = findings.filter { it.severity == AuditSeverity.ERROR }
        val detail = if (errors.isEmpty()) "no specification was produced" else
            errors.joinToString("; ") { "${it.code}: ${it.message}" }
        throw TrackedGeometryException("cannot use tracked mechanism: $detail")
    }
}
