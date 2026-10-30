package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Field validation for a tracked mechanism definition, with no model access.
 *
 * Keeping this separate from the geometry compiler makes the numeric contract
 * testable without an asset, and it stops a value like a negative damping ratio
 * from being folded into a "successful" spring row. It only checks whether a
 * value is *usable*; it never freezes a vehicle performance target.
 */
object TrackedDefinitionValidator {

    private const val AXIS_TOLERANCE = 1.0e-6

    fun validate(definition: TrackedVehicleDefinition): List<TrackedAuditFinding> {
        val findings = ArrayList<TrackedAuditFinding>()
        fun report(severity: AuditSeverity, code: String, message: String) =
            findings.add(TrackedAuditFinding(severity, code, message))
        val error: (String, String) -> Unit = { code, message -> report(AuditSeverity.ERROR, code, message) }
        val warn: (String, String) -> Unit = { code, message -> report(AuditSeverity.WARN, code, message) }

        if (definition.model.isBlank()) error("model", "no model path")
        if (!finite(definition.massKg) || definition.massKg <= 100.0) {
            error("mass", "MassKg must be a finite vehicle mass above 100 kg, was ${definition.massKg}")
        }
        if (!finite(definition.renderScale) || definition.renderScale !in 0.25..4.0) {
            error("render-scale", "RenderScale must be within 0.25..4.0, was ${definition.renderScale}")
        }
        validateVector(definition.chassisOrigin, "chassis-origin", "ChassisOrigin", error)

        val track = definition.track
        if (!finite(track.pitch) || track.pitch <= 0.001) {
            error("pitch", "Track.Pitch must be positive metres, was ${track.pitch}")
        }
        if (!finite(track.pinInset) || track.pinInset < 0.0) {
            error("pin-inset", "Track.PinInset must be a finite non-negative distance, was ${track.pinInset}")
        }
        if (!finite(track.clearance) || track.clearance < 0.0) {
            error("clearance", "Track.Clearance must be a finite non-negative gap, was ${track.clearance}")
        }
        if (!finite(track.angularLimitDegrees) || track.angularLimitDegrees !in 1.0..120.0) {
            error("angular-limit", "Track.AngularLimitDegrees out of range: ${track.angularLimitDegrees}")
        }
        if (track.linkCount < 0) {
            error("link-count", "Track.LinkCount must not be negative, was ${track.linkCount}")
        }
        if (track.linkCount == 0) {
            warn("link-count", "Track.LinkCount is 0; the solver will choose a count. Declare it before approval")
        }
        if (!finite(track.linkMass) || track.linkMass <= 0.0) {
            error("link-mass", "Track.LinkMass must be a positive mass; a shoe is a real body")
        }

        validateSide(TrackedSide.LEFT, definition.left, error, warn)
        validateSide(TrackedSide.RIGHT, definition.right, error, warn)
        validateUniqueBones(definition, error)

        validateRunBudget(definition.left, "LEFT", error)
        validateRunBudget(definition.right, "RIGHT", error)

        val drivetrain = definition.drivetrain
        requirePositive(drivetrain.maxDriveTorque, "drivetrain.torque", "MaxDriveTorque", error)
        requirePositive(drivetrain.maxBrakeTorque, "drivetrain.brake", "MaxBrakeTorque", error)
        requirePositive(drivetrain.maxSpeed, "drivetrain.speed", "MaxSpeed", error)
        requirePositive(drivetrain.differential, "drivetrain.differential", "Differential", error)
        if (!finite(drivetrain.rollingResistance) || drivetrain.rollingResistance !in 0.0..1.0) {
            error("drivetrain.rolling", "RollingResistance must be within 0..1, was ${drivetrain.rollingResistance}")
        }
        if (!finite(drivetrain.friction) || drivetrain.friction < 0.0) {
            error("drivetrain.friction", "Friction must be a finite non-negative value, was ${drivetrain.friction}")
        }
        validatePositiveResponseBound(drivetrain.accelMps2, "drivetrain.accel", "Accel", error)
        validatePositiveResponseBound(drivetrain.brakeMps2, "drivetrain.brake-rate", "Brake", error)
        validateResponseBound(drivetrain.pivotSpeedMps, "drivetrain.pivot", "PivotSpeed", error)
        drivetrain.steerTimeSeconds?.let {
            if (!finite(it) || it <= 0.0) {
                error("drivetrain.steer-time", "SteerTime must be a finite positive number of seconds, was $it")
            }
        }

        val terrain = definition.terrain
        if (terrain.windowRadiusBlocks !in 2..16) {
            error("terrain.radius", "Terrain.WindowRadius must be within 2..16 blocks, was ${terrain.windowRadiusBlocks}")
        }
        if (terrain.verticalAboveBlocks !in 2..16) {
            error("terrain.above", "Terrain.VerticalAbove must be within 2..16 blocks, was ${terrain.verticalAboveBlocks}")
        }
        if (terrain.verticalBelowBlocks !in 2..24) {
            error("terrain.below", "Terrain.VerticalBelow must be within 2..24 blocks, was ${terrain.verticalBelowBlocks}")
        }
        val ramps = terrain.bulletRamps
        ramps.maxClimbM?.let {
            if (!finite(it) || it <= 0.0) {
                error("terrain.max-climb", "BulletRamps.MaxClimb must be finite positive metres, was $it")
            }
        }
        ramps.stepM?.let {
            if (!finite(it) || it <= 0.001) {
                error("terrain.step", "BulletRamps.Step must be finite positive metres, was $it")
            }
        }
        if (ramps.enabled && ramps.maxClimbM == null) {
            error("terrain.max-climb", "BulletRamps.Enabled requires an explicit MaxClimb")
        }
        if (ramps.enabled && ramps.stepM == null) {
            error("terrain.step", "BulletRamps.Enabled requires an explicit Step")
        }

        return findings
    }

    private fun validateSide(
        side: TrackedSide,
        definition: TrackSideDefinition,
        error: (String, String) -> Unit,
        warn: (String, String) -> Unit,
    ) {
        val label = side.name
        if (definition.linkTemplateBone.isBlank()) {
            error("$label.template", "$label side declares no LinkTemplateBone")
        }
        if (definition.wheels.isEmpty()) {
            error("$label.empty", "$label side declares no wheels")
        }

        val suspension = definition.suspension
        validateSuspension(suspension, "$label.suspension", "$label", error)

        validateBeltOrder(definition, label, error)
        validateBeltWrap(definition, label, error)

        var drive = 0
        var idler = 0
        var support = 0
        var loadTotal = 0.0
        for (wheel in definition.wheels) {
            val bone = wheel.bone.ifBlank { "<unnamed>" }
            if (wheel.bone.isBlank()) error("$label.wheel.bone", "$label has a wheel without a bone")
            when (wheel.role) {
                TrackWheelRole.DRIVE_GUIDE -> drive++
                TrackWheelRole.IDLER_GUIDE -> idler++
                TrackWheelRole.SUPPORT -> support++
                TrackWheelRole.RETURN_ROLLER -> Unit
            }
            if (wheel.role != TrackWheelRole.IDLER_GUIDE && wheel.tensioner != null) {
                error(
                    "$label.tensioner.role",
                    "$bone declares a Tensioner but is ${wheel.role}; only an IdlerGuide may carry one",
                )
            }
            if (wheel.role == TrackWheelRole.SUPPORT) {
                if (!finite(wheel.loadShare) || wheel.loadShare <= 0.0) {
                    error("$label.wheel.load", "$bone has a non-positive LoadShare")
                } else {
                    loadTotal += wheel.loadShare
                }
            } else if (wheel.loadShare != 0.0 && finite(wheel.loadShare)) {
                warn(
                    "$label.wheel.load",
                    "$bone is ${wheel.role} but declares LoadShare ${wheel.loadShare}; it carries no ground load",
                )
            }
            if (!finite(wheel.radius) || wheel.radius <= 0.01) {
                error("$label.wheel.radius", "$bone needs a finite Radius above 0.01 m, was ${wheel.radius}")
            }
            if (!finite(wheel.halfWidth) || wheel.halfWidth <= 0.01) {
                error("$label.wheel.width", "$bone needs a finite HalfWidth above 0.01 m, was ${wheel.halfWidth}")
            }
            if (!finite(wheel.mass) || wheel.mass <= 0.0) {
                error("$label.wheel.mass", "$bone Mass must be a positive mass; a wheel is a real body")
            }
            validateAxis(wheel.axleDirection, "$label.wheel.axle", bone, error)
            wheel.suspension?.let { override ->
                validateSuspensionOverride(override, "$label.wheel.suspension", bone, error)
            }
            wheel.tensioner?.let { tensioner ->
                if (!finite(tensioner.travel) || tensioner.travel <= 0.0) {
                    error("$label.tensioner.travel", "$bone tensioner travel must be positive, was ${tensioner.travel}")
                }
                if (!finite(tensioner.preload) || tensioner.preload < 0.0) {
                    error("$label.tensioner.preload", "$bone tensioner preload must be non-negative")
                }
                if (!finite(tensioner.damping) || tensioner.damping < 0.0) {
                    error("$label.tensioner.damping", "$bone tensioner damping must be non-negative")
                }
                validateAxis(tensioner.axis, "$label.tensioner.axis", bone, error)
            }
        }

        if (drive != 1) error("$label.drive", "$label side needs exactly one DriveGuide, found $drive")
        if (idler != 1) error("$label.idler", "$label side needs exactly one IdlerGuide, found $idler")
        if (support < 2) error("$label.support", "$label side needs at least two Support wheels, found $support")
        if (support > 0 && !(loadTotal > 0.0)) {
            error("$label.wheel.load", "$label support wheels declare no usable LoadShare")
        }
    }

    /**
     * The traversal order is the physical topology, so it must be explicit.
     *
     * Every name must resolve, no name may repeat, and every authored wheel must
     * appear exactly once. A missing order is an error: falling back to a
     * longitudinal sort would invent a topology the vehicle never declared.
     */
    private fun validateBeltOrder(
        definition: TrackSideDefinition,
        label: String,
        error: (String, String) -> Unit,
    ) {
        if (definition.beltOrder.isEmpty()) {
            error(
                "$label.belt-order",
                "$label declares no BeltOrder; the run topology is that order and cannot be inferred",
            )
            return
        }
        if (definition.beltOrder.size < 2) {
            error("$label.belt-order", "$label BeltOrder needs at least two members")
        }
        val bones = definition.wheels.map { it.bone }.filter { it.isNotBlank() }.toSet()
        val seen = HashSet<String>()
        for (bone in definition.beltOrder) {
            if (!bones.contains(bone)) {
                error("$label.belt-order", "$label BeltOrder names unknown wheel $bone")
            }
            if (!seen.add(bone)) {
                error("$label.belt-order", "$label BeltOrder lists $bone twice")
            }
        }
        val missing = bones - seen
        if (missing.isNotEmpty()) {
            error(
                "$label.belt-order",
                "$label BeltOrder omits $missing; every authored wheel is a belt member in this schema",
            )
        }
    }

    /**
     * Wrap must be authored per member.
     *
     * A default of "outside" would be a silent physical decision: an inside wrap
     * puts the belt on the other side of the wheel and reverses the run's normal.
     */
    private fun validateBeltWrap(
        definition: TrackSideDefinition,
        label: String,
        error: (String, String) -> Unit,
    ) {
        for (wheel in definition.wheels) {
            if (wheel.wrap == null) {
                error(
                    "$label.wheel.wrap",
                    "${wheel.bone.ifBlank { "<unnamed>" }} declares no Wrap; outside and inside are different mechanisms",
                )
            }
        }
    }

    /**
     * The run budget is a mass, not a count: zero means "not authored" and is
     * reported, while a negative or non-finite value is an error.
     */
    private fun validateRunBudget(
        side: TrackSideDefinition,
        label: String,
        error: (String, String) -> Unit,
    ) {
        if (!finite(side.runMassBudget)) {
            error("$label.run-budget", "$label RunMassBudgetKg must be finite, was ${side.runMassBudget}")
        } else if (side.runMassBudget < 0.0) {
            error("$label.run-budget", "$label RunMassBudgetKg must not be negative, was ${side.runMassBudget}")
        }
    }

    private fun validateSuspension(
        suspension: TrackSuspensionDefinition,
        code: String,
        owner: String,
        error: (String, String) -> Unit,
    ) {
        if (!finite(suspension.travelUp) || suspension.travelUp <= 0.001) {
            error(code, "$owner TravelUp must exceed 0.001 m, was ${suspension.travelUp}")
        }
        if (!finite(suspension.travelDown) || suspension.travelDown <= 0.001) {
            error(code, "$owner TravelDown must exceed 0.001 m, was ${suspension.travelDown}")
        }
        if (!finite(suspension.frequency) || suspension.frequency <= 0.05) {
            error(code, "$owner Frequency must exceed 0.05 Hz, was ${suspension.frequency}")
        }
        suspension.springNPerM?.let {
            if (!finite(it) || it <= 0.0) {
                error(code, "$owner Spring must be a finite positive N/m value, was $it")
            }
        }
        suspension.damperNsPerM?.let {
            if (!finite(it) || it < 0.0) {
                error(code, "$owner Damper must be a finite non-negative N·s/m value, was $it")
            }
        }
        validateDamping(suspension.compressionDampingRatio, "$code.compression", owner, error)
        validateDamping(suspension.reboundDampingRatio, "$code.rebound", owner, error)
        if (!finite(suspension.maxForce) || suspension.maxForce <= 0.0) {
            error(code, "$owner MaxForce must be positive, was ${suspension.maxForce}")
        }
        if (!finite(suspension.bumpStopStart) || suspension.bumpStopStart !in 0.0..1.0) {
            error(code, "$owner BumpStopStart must be a fraction within 0..1, was ${suspension.bumpStopStart}")
        }
        if (!finite(suspension.bumpStopExponent) || suspension.bumpStopExponent < 1.0) {
            error(code, "$owner BumpStopExponent must be at least 1, was ${suspension.bumpStopExponent}")
        }
        validateAxis(suspension.direction, "$code.direction", owner, error)
    }

    private fun validateSuspensionOverride(
        override: TrackSuspensionOverride,
        code: String,
        owner: String,
        error: (String, String) -> Unit,
    ) {
        override.travelUp?.let {
            if (!finite(it) || it <= 0.001) error(code, "$owner TravelUp override must exceed 0.001 m, was $it")
        }
        override.travelDown?.let {
            if (!finite(it) || it <= 0.001) error(code, "$owner TravelDown override must exceed 0.001 m, was $it")
        }
        override.frequency?.let {
            if (!finite(it) || it <= 0.05) error(code, "$owner Frequency override must exceed 0.05 Hz, was $it")
        }
        override.springNPerM?.let {
            if (!finite(it) || it <= 0.0) {
                error(code, "$owner Spring override must be a finite positive N/m value, was $it")
            }
        }
        override.damperNsPerM?.let {
            if (!finite(it) || it < 0.0) {
                error(code, "$owner Damper override must be a finite non-negative N·s/m value, was $it")
            }
        }
        override.compressionDampingRatio?.let { validateDamping(it, code, owner, error) }
        override.reboundDampingRatio?.let { validateDamping(it, code, owner, error) }
        override.maxForce?.let {
            if (!finite(it) || it <= 0.0) error(code, "$owner MaxForce override must be positive, was $it")
        }
        override.bumpStopStart?.let {
            if (!finite(it) || it !in 0.0..1.0) {
                error(code, "$owner BumpStopStart override must be within 0..1, was $it")
            }
        }
        override.bumpStopExponent?.let {
            if (!finite(it) || it < 1.0) error(code, "$owner BumpStopExponent override must be at least 1, was $it")
        }
        override.direction?.let { validateAxis(it, code, owner, error) }
    }

    private fun validateDamping(value: Double, code: String, owner: String, error: (String, String) -> Unit) {
        if (!finite(value) || value < 0.0) {
            error(code, "$owner damping ratio must be a finite non-negative value, was $value")
        }
    }

    private fun requirePositive(
        value: Double,
        code: String,
        field: String,
        error: (String, String) -> Unit,
    ) {
        if (!finite(value) || value <= 0.0) error(code, "$field must be a finite positive value, was $value")
    }

    /** Nullable MCSP response bound: absent is fine, present must be finite and non-negative. */
    private fun validateResponseBound(
        value: Double?,
        code: String,
        field: String,
        error: (String, String) -> Unit,
    ) {
        value ?: return
        if (!finite(value) || value < 0.0) {
            error(code, "$field must be a finite non-negative value, was $value")
        }
    }

    /** MCSP `Accel`/`Brake` are strictly positive rates; zero would be ambiguous. */
    private fun validatePositiveResponseBound(
        value: Double?,
        code: String,
        field: String,
        error: (String, String) -> Unit,
    ) {
        value ?: return
        if (!finite(value) || value <= 0.0) {
            error(code, "$field must be a finite positive value, was $value")
        }
    }

    /**
     * A direction must be finite, non-zero, and axis aligned.
     *
     * The S1 solver is planar: the belt lives in the `(z, y)` plane and the
     * wheels turn about `x`. Accepting a canted axle here would silently project
     * it, which is exactly the kind of quiet approximation S1 is auditing.
     */
    private fun validateAxis(
        value: Vec3,
        code: String,
        owner: String,
        error: (String, String) -> Unit,
    ) {
        if (!finite(value)) {
            error(code, "$owner direction is not finite: $value")
            return
        }
        val length = sqrt(value.x * value.x + value.y * value.y + value.z * value.z)
        if (length < AXIS_TOLERANCE) {
            error(code, "$owner direction is degenerate: $value")
            return
        }
        val unit = value.scale(1.0 / length)
        val aligned = abs(abs(unit.x) - 1.0) < AXIS_TOLERANCE ||
            abs(abs(unit.y) - 1.0) < AXIS_TOLERANCE ||
            abs(abs(unit.z) - 1.0) < AXIS_TOLERANCE
        if (!aligned) error(code, "$owner direction must be axis aligned in the S1 planar model: $value")
    }

    private fun validateUniqueBones(
        definition: TrackedVehicleDefinition,
        error: (String, String) -> Unit,
    ) {
        val seen = HashMap<String, TrackedSide>()
        for (side in TrackedSide.entries) {
            for (wheel in definition.side(side).wheels) {
                if (wheel.bone.isBlank()) continue
                val previous = seen.put(wheel.bone, side)
                if (previous == side) {
                    error("${side.name}.wheel.duplicate", "${wheel.bone} is bound twice on the ${side.name} side")
                } else if (previous != null) {
                    error(
                        "wheel.duplicate",
                        "${wheel.bone} is bound on both the $previous and $side sides",
                    )
                }
            }
        }
    }

    private fun validateVector(
        value: Vec3,
        code: String,
        field: String,
        error: (String, String) -> Unit,
    ) {
        if (!finite(value)) error(code, "$field must be finite, was $value")
    }

    private fun finite(value: Vec3): Boolean =
        finite(value.x) && finite(value.y) && finite(value.z)

    private fun finite(value: Double): Boolean = value.isFinite()
}
