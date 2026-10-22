package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

/**
 * Data-driven description of one tracked vehicle.
 *
 * Everything here describes *mechanism*, not a particular algorithm. A vehicle
 * never selects a hand-written physics branch; it only binds model bones and
 * mechanical numbers. When a mechanism cannot be expressed here the correct fix
 * is a new shared capability, never a per-vehicle exception.
 *
 * Coordinates follow the authored Bedrock frame used by the rest of the
 * project: model pixels, X positive to the vehicle's left, Y up, Z forward.
 * The compiler converts them to metres exactly once while applying [renderScale].
 */
@Serializable
data class TrackedVehicleDefinition(
    /** Bedrock geometry owning this vehicle, e.g. `superbwarfare:models/bedrock/vehicle/ztz_99a.geo.json`. */
    @SerialName("Model")
    var model: String = "",

    /**
     * The renderer multiplies the whole baked model by this factor before it is
     * drawn. Physics must use the same factor or the wheels stop lining up with
     * the rendered belt. Applied exactly once, and validated to a sane range.
     */
    @SerialName("RenderScale")
    var renderScale: Double = 1.0,

    /**
     * Model-space point (metres, physics frame) that the Bullet chassis body
     * origin corresponds to — normally the `Collision` OBB centre plus the
     * centre-of-mass offset. Every compiled mechanism point is expressed
     * relative to it, because that is the frame the physics body uses.
     */
    @SerialName("ChassisOrigin")
    var chassisOrigin: SerializedVec3 = Vec3.ZERO,

    /** Complete vehicle mass in kilograms, including every running-gear body. */
    @SerialName("MassKg")
    var massKg: Double = 55_000.0,

    @SerialName("Track")
    var track: TrackMechanismDefinition = TrackMechanismDefinition(),

    @SerialName("Left")
    var left: TrackSideDefinition = TrackSideDefinition(),

    @SerialName("Right")
    var right: TrackSideDefinition = TrackSideDefinition(),

    @SerialName("Drivetrain")
    var drivetrain: TrackDrivetrainDefinition = TrackDrivetrainDefinition(),
) {
    fun side(side: TrackedSide): TrackSideDefinition = when (side) {
        TrackedSide.LEFT -> left
        TrackedSide.RIGHT -> right
    }
}

/** Mechanical properties shared by both belts of one vehicle. */
@Serializable
data class TrackMechanismDefinition(
    /**
     * Pin-to-pin pitch in metres. This is a machine dimension, not the length of
     * the authored link cube: a shoe model may be longer, shorter, or split.
     */
    @SerialName("Pitch")
    var pitch: Double = 0.23307,

    /**
     * Distance from the pin axis to the link's inner wearing surface, in metres
     * along the link's local up axis. Pins near the inner face keep a plain
     * rectangular shoe from interpenetrating when the belt wraps inward.
     */
    @SerialName("PinInset")
    var pinInset: Double = 0.0,

    /**
     * Maximum rotation between two neighbouring links. The compiler also
     * reports the wrap angle the wheels actually demand, so this limit can be
     * audited against the mechanism instead of guessed.
     */
    @SerialName("AngularLimitDegrees")
    var angularLimitDegrees: Double = 40.0,

    /**
     * Declared link count per side. Zero means "use the solver's count"; that is
     * only acceptable while experimenting. An approved vehicle declares the
     * audited number explicitly.
     */
    @SerialName("LinkCount")
    var linkCount: Int = 0,

    /** Extra clearance added to the solved pin locus, in metres. */
    @SerialName("Clearance")
    var clearance: Double = 0.0,

    /**
     * Mass of a single shoe in kilograms. This is a real body in the simulation,
     * so it must be subtracted from the sprung mass. Zero means "unspecified";
     * the compiler then reports an error rather than inventing a share.
     */
    @SerialName("LinkMass")
    var linkMass: Double = 0.0,
)

/** One side's running gear and its own link template. */
@Serializable
data class TrackSideDefinition(
    /**
     * Bone owning the single-link mesh used for every physical shoe, for
     * example `trackRotL0`. The compiler extracts that bone and its cubes.
     */
    @SerialName("LinkTemplateBone")
    var linkTemplateBone: String = "",

    /**
     * Optional bone-name prefix of the visible link bones for this side, for
     * example `trackRotL`. When absent no visible description is produced; the
     * physics layer never guesses a render template from bone names.
     */
    @SerialName("VisibleBonePrefix")
    var visibleBonePrefix: String? = null,

    @SerialName("Suspension")
    var suspension: TrackSuspensionDefinition = TrackSuspensionDefinition(),

    @SerialName("Wheels")
    var wheels: MutableList<TrackWheelDefinition> = mutableListOf(),
)

/**
 * Wheel roles. The physics layer never infers a role from a bone name, an index
 * or a "front/rear" assumption: the definition states it.
 */
enum class TrackWheelRole {
    /** Sprung road wheel carrying the hull. */
    @SerialName("Support")
    SUPPORT,

    /** Sprocket that receives drive torque; may sit at either end. */
    @SerialName("DriveGuide")
    DRIVE_GUIDE,

    /** Passive end wheel; the one that can carry a tensioner. */
    @SerialName("IdlerGuide")
    IDLER_GUIDE,

    /** Optional fixed-axis return roller. Not sprung, does not touch the ground. */
    @SerialName("ReturnRoller")
    RETURN_ROLLER,
}

enum class TrackedSide { LEFT, RIGHT }

@Serializable
data class TrackWheelDefinition(
    /** Explicit model bone, e.g. `wheelL3`. Never resolved by traversal order. */
    @SerialName("Bone")
    var bone: String = "",

    @SerialName("Role")
    var role: TrackWheelRole = TrackWheelRole.SUPPORT,

    /**
     * Static load share used for spring preload. Relative weights are
     * normalised per side, so an uneven wheel base can be described without
     * hard-coding "divide by six".
     */
    @SerialName("LoadShare")
    var loadShare: Double = 1.0,

    @SerialName("Suspension")
    var suspension: TrackSuspensionOverride? = null,

    @SerialName("Tensioner")
    var tensioner: TrackTensionerDefinition? = null,

    /** Mass of this wheel body in kilograms. */
    @SerialName("Mass")
    var mass: Double = 0.0,

    /**
     * Rolling radius in metres. Authored explicitly: a wheel bone's subtree can
     * contain a suspension arm, so deriving the radius from mesh extents would
     * measure the arm instead of the tire.
     */
    @SerialName("Radius")
    var radius: Double = 0.0,

    /** Lateral half width in metres. */
    @SerialName("HalfWidth")
    var halfWidth: Double = 0.0,

    /**
     * Wheel axle direction in the chassis frame. A direction, not a position, so
     * both sides use `+X`. The S1 planar solver only accepts a lateral axle;
     * other directions are reported instead of being silently projected.
     */
    @SerialName("AxleDirection")
    var axleDirection: SerializedVec3 = Vec3(1.0, 0.0, 0.0),
)

/**
 * Spring/damper description. The compiler converts these to the SI quantities
 * the solver needs; the physics layer never reuses a "damping ratio" value as if
 * it were N·s/m.
 */
@Serializable
data class TrackSuspensionDefinition(
    @SerialName("TravelUp")
    var travelUp: Double = 0.18,

    @SerialName("TravelDown")
    var travelDown: Double = 0.24,

    /** Undamped natural frequency in Hz. */
    @SerialName("Frequency")
    var frequency: Double = 2.2,

    /** Dimensionless compression damping ratio. */
    @SerialName("CompressionDampingRatio")
    var compressionDampingRatio: Double = 0.65,

    /** Dimensionless rebound damping ratio. */
    @SerialName("ReboundDampingRatio")
    var reboundDampingRatio: Double = 0.45,

    /** Hard limit for one wheel's spring row, in newtons. */
    @SerialName("MaxForce")
    var maxForce: Double = 400_000.0,

    /** Fraction of `travelUp` where the progressive bump stop starts. */
    @SerialName("BumpStopStart")
    var bumpStopStart: Double = 0.85,

    @SerialName("BumpStopExponent")
    var bumpStopExponent: Double = 2.0,

    /**
     * Suspension travel direction in the chassis frame. `+Y` is compression.
     * The S1 solver only accepts a vertical axis, so a canted suspension is
     * reported rather than approximated.
     */
    @SerialName("Direction")
    var direction: SerializedVec3 = Vec3(0.0, 1.0, 0.0),
)

@Serializable
data class TrackSuspensionOverride(
    @SerialName("TravelUp")
    var travelUp: Double? = null,

    @SerialName("TravelDown")
    var travelDown: Double? = null,

    @SerialName("Frequency")
    var frequency: Double? = null,

    @SerialName("CompressionDampingRatio")
    var compressionDampingRatio: Double? = null,

    @SerialName("ReboundDampingRatio")
    var reboundDampingRatio: Double? = null,

    @SerialName("MaxForce")
    var maxForce: Double? = null,

    @SerialName("BumpStopStart")
    var bumpStopStart: Double? = null,

    @SerialName("BumpStopExponent")
    var bumpStopExponent: Double? = null,

    @SerialName("Direction")
    var direction: SerializedVec3? = null,
) {
    fun applyTo(base: TrackSuspensionDefinition): TrackSuspensionDefinition = base.copy(
        travelUp = travelUp ?: base.travelUp,
        travelDown = travelDown ?: base.travelDown,
        frequency = frequency ?: base.frequency,
        compressionDampingRatio = compressionDampingRatio ?: base.compressionDampingRatio,
        reboundDampingRatio = reboundDampingRatio ?: base.reboundDampingRatio,
        maxForce = maxForce ?: base.maxForce,
        bumpStopStart = bumpStopStart ?: base.bumpStopStart,
        bumpStopExponent = bumpStopExponent ?: base.bumpStopExponent,
        direction = direction ?: base.direction,
    )
}

/**
 * Passive tensioner: a preload plus viscous damping acting on the idler along
 * the side's longitudinal axis, bounded by a hard mechanical travel.
 */
@Serializable
data class TrackTensionerDefinition(
    /** Symmetric travel around the authored bind position, in metres. */
    @SerialName("Travel")
    var travel: Double = 0.10,

    @SerialName("Preload")
    var preload: Double = 20_000.0,

    /** Viscous coefficient in N·s/m. */
    @SerialName("Damping")
    var damping: Double = 8_000.0,

    /**
     * Free-travel axis in the chassis frame. The S1 solver only moves the idler
     * along the belt's longitudinal axis, so any other axis is reported.
     */
    @SerialName("Axis")
    var axis: SerializedVec3 = Vec3(0.0, 0.0, 1.0),
)

@Serializable
data class TrackDrivetrainDefinition(
    @SerialName("MaxDriveTorque")
    var maxDriveTorque: Double = 160_000.0,

    @SerialName("MaxBrakeTorque")
    var maxBrakeTorque: Double = 320_000.0,

    @SerialName("MaxSpeed")
    var maxSpeed: Double = 15.6,

    /** Left/right differential split authored by the vehicle. */
    @SerialName("Differential")
    var differential: Double = 1.7,

    @SerialName("RollingResistance")
    var rollingResistance: Double = 0.015,

    @SerialName("Friction")
    var friction: Double = 0.8,
)
