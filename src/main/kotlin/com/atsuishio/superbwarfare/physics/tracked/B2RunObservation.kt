package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * B2.0: the observation contract for a dynamic track run.
 *
 * B2 has to answer two separate questions, and this file keeps them apart:
 *
 * 1. **Is the assembled chain the audited one?** That is a pure-geometry
 *    question and it can be answered at T0 without Bullet. [B2InitialAssembly]
 *    does exactly that.
 * 2. **Does it stay the audited one while carrying load?** That needs Bullet and
 *    lives in [B2RunObservation], which records what a runtime measurement must
 *    report and rejects a record that silently omits a field.
 *
 * Nothing here creates a Bullet object, reads a Level, or applies a force. It is
 * also not an acceptance claim: [B2RunObservation.problems] only enforces
 * internal consistency, never a performance threshold. Thresholds belong in
 * `ACCEPTANCE_LIMITS.md` §2.2 and are still unfrozen.
 *
 * A deliberate omission: there is no joint-load field. Bullet's 6-Dof
 * `appliedImpulse` reads back 0 regardless of load, so a "joint force" column
 * here would be fabricated. Load evidence must come from contact impulses and
 * the suspension row, which is why [B2RunObservation] carries
 * [B2RunObservation.suspensionForceN] but no hinge impulse.
 */
object B2Sampling {
    /** Physics substep, seconds. Matches the core's fixed `1/60 s`. */
    const val SUBSTEP_SECONDS = 1.0 / 60.0

    /** Short-chain settle window (B2.1), in substeps. */
    const val SHORT_CHAIN_SUBSTEPS = 300

    /** Full-density settle window (B2.2–B2.4), in substeps. */
    const val SETTLE_SUBSTEPS = 600

    /** Pin coincidence the assembled pose must reproduce, metres. */
    const val INITIAL_PIN_TOLERANCE_M = 1.0e-6

    /**
     * Swing (locked-axis) misalignment the assembled pose may start with,
     * radians. This is an assembly sanity bound, not a B2 acceptance limit: a
     * hinge that starts with visibly skewed locked axes is a spec defect.
     */
    const val INITIAL_FRAME_TOLERANCE_RAD = 1.0e-3
}

/**
 * Measured result of assembling one audited side's spec, no physics involved.
 *
 * Both values are maxima over all hinges, because one bad hinge is enough to
 * make the run start pre-stressed.
 */
data class B2InitialAssembly(
    val side: TrackedSide,
    val linkCount: Int,
    val jointCount: Int,
    /** Largest distance between the two world pins of one hinge, metres. */
    val worstPinGapM: Double,
    /** Largest swing angle between the two hinges' locked axes, radians. */
    val worstFrameAngleRad: Double,
    /** Hinge with the largest pin gap, for diagnostics. */
    val worstPinJoint: Int,
    /** Hinge with the largest frame misalignment, for diagnostics. */
    val worstFrameJoint: Int,
) {
    val pinCoincides: Boolean get() = worstPinGapM <= B2Sampling.INITIAL_PIN_TOLERANCE_M

    val framesAligned: Boolean get() = worstFrameAngleRad <= B2Sampling.INITIAL_FRAME_TOLERANCE_RAD

    /** Structural problems only; thresholds live in `ACCEPTANCE_LIMITS.md`. */
    fun problems(): List<String> = buildList {
        if (linkCount <= 0) add("assembly has no links")
        if (jointCount != linkCount) add("expected one hinge per link, got $jointCount for $linkCount links")
        if (!worstPinGapM.isFinite() || worstPinGapM < 0.0) add("worstPinGapM is not a finite distance: $worstPinGapM")
        if (!worstFrameAngleRad.isFinite() || worstFrameAngleRad < 0.0) {
            add("worstFrameAngleRad is not a finite angle: $worstFrameAngleRad")
        }
        if (worstPinJoint !in 0 until jointCount.coerceAtLeast(1)) add("worstPinJoint out of range: $worstPinJoint")
        if (worstFrameJoint !in 0 until jointCount.coerceAtLeast(1)) add("worstFrameJoint out of range: $worstFrameJoint")
    }
}

/**
 * B2.0 measurement of the assembled run.
 *
 * It deliberately takes a [TrackRunSpec] rather than raw numbers, and refuses a
 * spec whose links do not match the compiled side. That is the mechanical
 * enforcement of "`TrackedRunSpecs` is the only spec producer": a fixture cannot
 * quietly assemble a different, easier chain and still be measured here.
 */
object B2AssemblyProbe {

    /** Throws if [spec] was not produced from [compiled]. */
    fun requireMatches(spec: TrackRunSpec, compiled: CompiledTrackSide) {
        require(spec.links.size == compiled.placements.size) {
            "spec has ${spec.links.size} links but ${compiled.side} compiled ${compiled.placements.size}"
        }
        require(spec.joints.size == compiled.placements.size) {
            "spec has ${spec.joints.size} hinges for ${compiled.placements.size} links"
        }
        val expectedMass = compiled.linkMassKg.toFloat()
        spec.links.forEachIndexed { index, link ->
            require(link.massKg == expectedMass) {
                "link $index mass ${link.massKg} != audited $expectedMass"
            }
            val shape = link.shape as? TrackRunLinkShape.Box
                ?: throw IllegalArgumentException("link $index is not the audited box shoe")
            require(shape.halfExtents == compiled.linkTemplate.physical.halfExtents) {
                "link $index half extents ${shape.halfExtents} != audited ${compiled.linkTemplate.physical.halfExtents}"
            }
            val placement = compiled.placements[index]
            require(link.position.distanceToSqr(placement.center) <= PLACEMENT_TOLERANCE_SQR) {
                "link $index pose ${link.position} != audited placement ${placement.center}"
            }
        }
    }

    /**
     * Measure pin coincidence and locked-axis alignment of the bind pose.
     *
     * Pin coincidence is the invariant the runtime needs: `New6Dof` is handed
     * two pivots that must land on the same world point. The hinge axes are the
     * locked part of the constraint, so if the neighbouring frames disagree the
     * solver starts by fighting a rotation that the authored pose never asked
     * for.
     */
    fun measure(spec: TrackRunSpec, compiled: CompiledTrackSide): B2InitialAssembly {
        requireMatches(spec, compiled)
        var worstPin = 0.0
        var worstPinJoint = 0
        var worstFrame = 0.0
        var worstFrameJoint = 0

        spec.joints.forEachIndexed { index, joint ->
            val previous = spec.links[joint.previousIndex]
            val next = spec.links[joint.nextIndex]

            val previousPin = pinPoint(previous, joint.pivotInPrevious)
            val nextPin = pinPoint(next, joint.pivotInNext)
            val gap = previousPin.distanceTo(nextPin)
            if (gap > worstPin) {
                worstPin = gap
                worstPinJoint = index
            }

            val previousAxis = hingeAxis(previous)
            val nextAxis = hingeAxis(next)
            val swing = angleBetween(previousAxis, nextAxis)
            if (swing > worstFrame) {
                worstFrame = swing
                worstFrameJoint = index
            }
        }

        return B2InitialAssembly(
            side = compiled.side,
            linkCount = spec.links.size,
            jointCount = spec.joints.size,
            worstPinGapM = worstPin,
            worstFrameAngleRad = worstFrame,
            worstPinJoint = worstPinJoint,
            worstFrameJoint = worstFrameJoint,
        )
    }

    /**
     * World position of one hinge pin, given the link's authored pose.
     *
     * The hinge axis is local X for every 99A hinge, so the locked axis in world
     * is the body's X axis. Comparing the two bodies' world X axes is therefore
     * an exact measure of how much non-hinge rotation the bind pose demands.
     */
    private fun pinPoint(
        link: cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec,
        local: Vec3,
    ): Vec3 {
        val rotated = link.rotation.transform(Vector3f(local.x.toFloat(), local.y.toFloat(), local.z.toFloat()))
        return Vec3(link.position.x + rotated.x, link.position.y + rotated.y, link.position.z + rotated.z)
    }

    private fun hingeAxis(link: cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec): Vector3f =
        link.rotation.transform(Vector3f(1f, 0f, 0f)).normalize()

    private fun angleBetween(a: Vector3f, b: Vector3f): Double {
        val dot = (a.x * b.x + a.y * b.y + a.z * b.z).toDouble()
        return acos(dot.coerceIn(-1.0, 1.0))
    }

    private const val PLACEMENT_TOLERANCE_SQR = 1.0e-18
}

/**
 * One dynamic measurement of a running chain.
 *
 * Every field is required. A record that cannot supply one is not a B2 record:
 * the alternative is an optional field that quietly defaults to a comforting
 * zero, which is how "it looks stable" becomes an unverifiable claim.
 *
 * Units are in the field names on purpose. This file is the place where the
 * measurement contract lives, so a reader never has to guess whether a number is
 * metres, radians, newtons or counts.
 */
data class B2RunObservation(
    val side: TrackedSide,
    /** Which chain produced this record. Only [B2ChainDensity.FULL] can support a pass. */
    val density: B2ChainDensity,
    /** Substeps elapsed since release, at [B2Sampling.SUBSTEP_SECONDS] each. */
    val substeps: Int,
    /** Largest distance between the two pins of one hinge during the run, metres. */
    val worstPinGapM: Double,
    /** Largest angle outside a hinge's declared window, radians. Zero inside limits. */
    val worstJointAngleViolationRad: Double,
    /** Number of road wheels that did receive at least one link contact. */
    val linkWheelContactCount: Int,
    /** Number of wheels the side actually has, so a zero contact count is meaningful. */
    val wheelCount: Int,
    /** Deepest link-into-wheel overlap observed, metres. Zero means no overlap. */
    val worstLinkWheelPenetrationM: Double,
    /** Number of links that received at least one terrain contact. */
    val linkTerrainContactCount: Int,
    /** Cross-link contacts within the same owner. Must stay zero. */
    val linkLinkContactCount: Int,
    /**
     * Longitudinal slip between the first and last link over the window, metres.
     * Absolute value; sign carries no meaning for a closed loop.
     */
    val longitudinalDriftM: Double,
    /** Per-station suspension travel, metres, in `CompiledWheel.index` order. */
    val suspensionTravelM: List<Double>,
    /** Per-station suspension row force, newtons, positive in compression. */
    val suspensionForceN: List<Double>,
    /** Chassis vertical displacement from its release pose, metres. */
    val chassisHeaveM: Double,
    /** Chassis pitch change from its release pose, radians. */
    val chassisPitchRad: Double,
) {
    val linkWheelCoverage: Double
        get() = if (wheelCount <= 0) 0.0 else linkWheelContactCount.toDouble() / wheelCount

    /** True when the record came from a full-density chain, not a diagnostic shortcut. */
    val isFullDensity: Boolean get() = density == B2ChainDensity.FULL

    /**
     * Structural problems with this record, not performance judgements.
     *
     * These catch the failures that make a run meaningless: a hand-sampled
     * window, a chain that touches itself, a station list that does not match
     * the wheels, or a value that is not a number. Thresholds such as
     * `trackRunDrift` are applied later, against frozen limits.
     */
    fun problems(expectedStations: Int): List<String> = buildList {
        if (substeps < 0) add("substeps must not be negative: $substeps")
        if (wheelCount <= 0) add("wheelCount must be positive: $wheelCount")
        if (linkWheelContactCount !in 0..wheelCount) {
            add("linkWheelContactCount $linkWheelContactCount is outside 0..$wheelCount")
        }
        if (linkTerrainContactCount < 0) add("linkTerrainContactCount must not be negative")
        if (linkLinkContactCount != 0) {
            add("same-owner links touched each other $linkLinkContactCount times")
        }
        if (suspensionTravelM.size != expectedStations) {
            add("suspensionTravelM has ${suspensionTravelM.size} entries, expected $expectedStations")
        }
        if (suspensionForceN.size != expectedStations) {
            add("suspensionForceN has ${suspensionForceN.size} entries, expected $expectedStations")
        }
        val nonFinite = listOf(
            "worstPinGapM" to worstPinGapM,
            "worstJointAngleViolationRad" to worstJointAngleViolationRad,
            "worstLinkWheelPenetrationM" to worstLinkWheelPenetrationM,
            "longitudinalDriftM" to longitudinalDriftM,
            "chassisHeaveM" to chassisHeaveM,
            "chassisPitchRad" to chassisPitchRad,
        ).filter { !it.second.isFinite() }
        nonFinite.forEach { add("${it.first} is not finite: ${it.second}") }
        if (worstJointAngleViolationRad < 0.0) add("joint angle violation cannot be negative")
        if (worstLinkWheelPenetrationM < 0.0) add("penetration is reported as a non-negative depth")
        if (longitudinalDriftM < 0.0) add("longitudinal drift is reported as a magnitude")
        if (suspensionTravelM.any { !it.isFinite() }) add("suspensionTravelM contains a non-finite value")
        if (suspensionForceN.any { !it.isFinite() }) add("suspensionForceN contains a non-finite value")
    }

    /** Diagnostics line for a test report; contains no verdict. */
    fun describe(): String = buildString {
        append("$side ")
        append("substeps=$substeps ")
        append("pinGap=%.3e m ".format(worstPinGapM))
        append("angleViolation=%.3e rad ".format(worstJointAngleViolationRad))
        append("wheelContacts=$linkWheelContactCount/$wheelCount ")
        append("penetration=%.4f m ".format(worstLinkWheelPenetrationM))
        append("terrainLinks=$linkTerrainContactCount ")
        append("linkLink=$linkLinkContactCount ")
        append("drift=%.4f m ".format(longitudinalDriftM))
        append("travel=${suspensionTravelM.map { "%.4f".format(it) }} ")
        append("force=${suspensionForceN.map { "%.0f".format(it) }} ")
        append("heave=%.4f m ".format(chassisHeaveM))
        append("pitch=%.4f rad".format(chassisPitchRad))
    }
}

/** Which chain a fixture is allowed to claim. */
enum class B2ChainDensity {
    /** The audited 79 links per side. The only density that can support a pass. */
    FULL,

    /**
     * A deliberately reduced chain used to localise an instability.
     * A [B2ChainDensity.DIAGNOSTIC] run may be recorded but can never be cited
     * as B2 evidence for the vehicle.
     */
    DIAGNOSTIC,
}

/** Helper so a diagnostic run cannot accidentally be reported as full density. */
fun B2ChainDensity.describe(linkCount: Int, auditedLinkCount: Int): String = when (this) {
    B2ChainDensity.FULL -> {
        require(linkCount == auditedLinkCount) {
            "FULL density requires $auditedLinkCount links, got $linkCount"
        }
        "full density, $linkCount links"
    }
    B2ChainDensity.DIAGNOSTIC ->
        "DIAGNOSTIC ONLY, $linkCount of $auditedLinkCount links; not acceptance evidence"
}
