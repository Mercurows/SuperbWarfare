package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import java.util.Collections

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
     * Position the closed loop actually uses. It equals [bindCenter] for every
     * wheel except the idler, which carries the solved tensioner offset.
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

/** One side's compiled mechanism. Immutable; every list is defensively copied. */
class CompiledTrackSide(
    val side: TrackedSide,
    wheels: List<CompiledWheel>,
    val linkTemplate: CompiledLinkTemplate,
    val loop: ClosedTrackLoopSolver.Result,
    val driveIndex: Int,
    val idlerIndex: Int,
    /** Mass of one shoe, kg. */
    val linkMassKg: Double,
    placements: List<CompiledLinkPlacement>,
    val geometry: CompiledGeometryAudit,
    /** Independent suspension rows for the road wheels of this side. */
    val suspension: SuspensionAudit,
) {
    val wheels: List<CompiledWheel> = wheels.toList()
    val placements: List<CompiledLinkPlacement> = placements.toList()

    val linkMassTotalKg: Double get() = linkMassKg * loop.linkCount
    val wheelMassTotalKg: Double get() = wheels.sumOf { it.massKg }
    val runningGearMassKg: Double get() = linkMassTotalKg + wheelMassTotalKg
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
)

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
    val hullMassKg: Double,
    val runningGearMassKg: Double,
    val totalMassKg: Double,
    /** One representative shoe per side. */
    val shoes: List<CompiledBodyMass>,
    val wheels: List<CompiledBodyMass>,
)

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
    val massKg: Double,
    val pitch: Double,
    val pinInset: Double,
    /** Authored minimum static shoe gap, metres. */
    val clearanceM: Double,
    val linkMassKg: Double,
    val angularLimitDegrees: Double,
    val drivetrain: CompiledDrivetrain,
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

    /** Total number of physically simulated shoes, both sides. */
    val totalLinkCount: Int get() = sides.values.sumOf { it.loop.linkCount }

    /** Total number of dynamic wheels, both sides. */
    val totalWheelCount: Int get() = sides.values.sumOf { it.wheels.size }

    /** Sum of every sprung station mass; must stay below [massKg]. */
    val stationMassTotal: Double get() = sides.values.sumOf { side ->
        side.wheels.filter { it.isSuspended }.sumOf { it.suspension.stationMassKg }
    }

    /** Every link plus every wheel body, both sides. */
    val runningGearMassKg: Double get() = sides.values.sumOf { it.runningGearMassKg }

    /** Hull mass carried by the springs. */
    val sprungMassKg: Double
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
