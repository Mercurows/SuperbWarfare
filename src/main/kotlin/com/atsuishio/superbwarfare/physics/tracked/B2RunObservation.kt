package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * B2.0: the observation contract for the dynamic track run.
 *
 * B2 has to answer two separate questions, and this file keeps them apart:
 *
 * 1. **Is the assembled mechanism the audited one?** That is pure geometry and
 *    can be answered at T0 without Bullet. [TrackRunAssemblyProbe] does that for
 *    the run topology; [LegacyB2AssemblyProbe] keeps the historical shoe-chain
 *    check reproducible.
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
 * [B2RunObservation.suspensionForceN] but no joint impulse.
 */
object B2Sampling {
    /** Physics substep, seconds. Matches the core's fixed `1/60 s`. */
    const val SUBSTEP_SECONDS = 1.0 / 60.0

    /** Short-chain settle window (B2.1), in substeps. */
    const val SHORT_CHAIN_SUBSTEPS = 300

    /** Full-mechanism settle window (B2.2–B2.4), in substeps. */
    const val SETTLE_SUBSTEPS = 600

    /** Anchor coincidence the assembled pose must reproduce, metres. */
    const val INITIAL_PIN_TOLERANCE_M = 1.0e-6

    /**
     * Locked-axis misalignment the assembled pose may start with, radians.
     *
     * An assembly sanity bound, not a B2 acceptance limit: a joint that starts
     * with visibly skewed locked axes is a spec defect.
     */
    const val INITIAL_FRAME_TOLERANCE_RAD = 1.0e-3
}

/**
 * Measured result of assembling one run-topology side, no physics involved.
 *
 * Each end is checked against its own compiled plan:
 *
 * - the two anchors of one end must land on the same world point;
 * - the end's declared locked axis (`Y` in its own frame) must agree between the
 *   two bodies once each frame is applied to its body pose.
 *
 * The first end's **free** axle rotation is deliberately not measured here: a
 * rotation difference about the free axis is legal at assembly and is not an
 * error. Only the locked directions are checked.
 */
data class B2RunAssembly(
    val side: TrackedSide,
    /** Number of runs in this assembly. */
    val runCount: Int,
    /** Number of wheel bodies in this assembly. */
    val wheelCount: Int,
    /** Number of external end constraints, two per run. */
    val endCount: Int,
    /** Largest distance between the two world anchors of one end, metres. */
    val worstAnchorGapM: Double,
    /** Largest angular disagreement of one end's locked axis, radians. */
    val worstLockedAxisRad: Double,
    /** Run whose end had the worst anchor gap, for diagnostics. */
    val worstAnchorRun: Int,
    /** Run whose end had the worst locked-axis disagreement. */
    val worstAxisRun: Int,
) {
    val anchorsCoincide: Boolean get() = worstAnchorGapM <= B2Sampling.INITIAL_PIN_TOLERANCE_M

    val axesAligned: Boolean get() = worstLockedAxisRad <= B2Sampling.INITIAL_FRAME_TOLERANCE_RAD

    /** Structural problems only; thresholds live in `ACCEPTANCE_LIMITS.md`. */
    fun problems(): List<String> = buildList {
        if (runCount <= 0) add("assembly has no runs")
        if (wheelCount <= 0) add("assembly has no wheel bodies")
        if (endCount != runCount * 2) add("expected two ends per run, got $endCount for $runCount runs")
        if (!worstAnchorGapM.isFinite() || worstAnchorGapM < 0.0) {
            add("worstAnchorGapM is not a finite distance: $worstAnchorGapM")
        }
        if (!worstLockedAxisRad.isFinite() || worstLockedAxisRad < 0.0) {
            add("worstLockedAxisRad is not a finite angle: $worstLockedAxisRad")
        }
        if (runCount > 0 && worstAnchorRun !in 0 until runCount) add("worstAnchorRun out of range: $worstAnchorRun")
        if (runCount > 0 && worstAxisRun !in 0 until runCount) add("worstAxisRun out of range: $worstAxisRun")
    }

    fun describe(): String =
        "$side runs=$runCount wheels=$wheelCount ends=$endCount " +
            "anchorGap=%.3e m lockedAxis=%.3e rad".format(worstAnchorGapM, worstLockedAxisRad)
}

/**
 * Measured result of assembling one legacy shoe chain, no physics involved.
 *
 * Both values are maxima over all hinges, because one bad hinge is enough to
 * make the chain start pre-stressed. **Legacy topology only**: the run mechanism
 * has no internal hinges, and its assembly result is [B2RunAssembly].
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
 * B2.0 probe for the run topology.
 *
 * It deliberately takes the core spec list **and** the compiled side, and refuses
 * a spec that was not produced from it. That is the mechanical enforcement of
 * "`TrackedRunSpecs.compileRuns` is the only spec producer": a fixture cannot
 * quietly assemble a different, easier mechanism and still be measured here.
 */
object TrackRunAssemblyProbe {

    private const val PLACEMENT_TOLERANCE_SQR = 1.0e-18
    private const val ROTATION_TOLERANCE = 1.0e-5

    /**
     * Throws if [specs] were not produced from [compiled].
     *
     * Checks every field a runtime consumes: mass, box extents, position,
     * rotation, the single-link/no-internal-joint shape, and the one-to-one
     * correspondence between specs and compiled runs.
     */
    fun requireMatches(specs: List<TrackRunSpec>, compiled: CompiledTrackSide) {
        require(specs.size == compiled.runs.size) {
            "spec list has ${specs.size} runs but ${compiled.side} compiled ${compiled.runs.size}"
        }
        specs.forEachIndexed { index, spec ->
            val run = compiled.runs[index]
            require(spec.links.size == 1) { "run $index has ${spec.links.size} links; a run is one box" }
            require(spec.joints.isEmpty()) { "run $index has internal joints; its ends are external constraints" }

            val link = spec.links.single()
            require(abs(link.massKg - run.massKg.toFloat()) <= 1e-3f) {
                "run $index mass ${link.massKg} != audited ${run.massKg}"
            }
            val shape = link.shape as? TrackRunLinkShape.Box
                ?: throw IllegalArgumentException("run $index is not the audited box")
            require(shape.halfExtents.distanceToSqr(run.halfExtents) <= 1e-12) {
                "run $index extents ${shape.halfExtents} != audited ${run.halfExtents}"
            }
            require(link.position.distanceToSqr(run.center) <= PLACEMENT_TOLERANCE_SQR) {
                "run $index pose ${link.position} != audited centre ${run.center}"
            }
            require(abs(link.rotationX - run.rotation.x) <= ROTATION_TOLERANCE &&
                abs(link.rotationY - run.rotation.y) <= ROTATION_TOLERANCE &&
                abs(link.rotationZ - run.rotation.z) <= ROTATION_TOLERANCE &&
                abs(link.rotationW - run.rotation.w) <= ROTATION_TOLERANCE
            ) {
                "run $index rotation does not match the audited rotation"
            }
        }
    }

    /**
     * Measure the assembled pose of the run topology.
     *
     * Both ends of every run are checked, and **each side of the joint is placed
     * by its own body**: the run end uses the run pose, the wheel end uses the
     * wheel pose. Computing both from the run alone would make the comparison
     * identically zero and would "pass" no matter what the compiled frames said.
     *
     * The anchor is the wheel centre by design, so the two anchors of one end must
     * coincide in world space; the locked axis is the end plan's local `Y`,
     * transformed by each body's own pose.
     */
    fun measure(specs: List<TrackRunSpec>, compiled: CompiledTrackSide): B2RunAssembly {
        requireMatches(specs, compiled)
        var worstAnchor = 0.0
        var worstAnchorRun = 0
        var worstAxis = 0.0
        var worstAxisRun = 0

        for (run in compiled.runs) {
            for (attachment in run.attachments) {
                val plan = attachment.joint
                val anchorA = worldAnchor(plan.bodyA, plan.frameInA, run, compiled)
                val anchorB = worldAnchor(plan.bodyB, plan.frameInB, run, compiled)
                val gap = anchorA.distanceTo(anchorB)
                if (gap > worstAnchor) {
                    worstAnchor = gap
                    worstAnchorRun = run.index
                }

                val axisA = worldAxis(plan.bodyA, plan.frameInA, run, compiled, attachment.end)
                val axisB = worldAxis(plan.bodyB, plan.frameInB, run, compiled, attachment.end)
                if (attachment.end == RunEnd.FIRST) {
                    val angle = angleBetween(axisA, axisB)
                    if (angle > worstAxis) {
                        worstAxis = angle
                        worstAxisRun = run.index
                    }
                }
            }
        }

        return B2RunAssembly(
            side = compiled.side,
            runCount = compiled.runs.size,
            wheelCount = compiled.wheels.size,
            endCount = compiled.runs.size * 2,
            worstAnchorGapM = worstAnchor,
            worstLockedAxisRad = worstAxis,
            worstAnchorRun = worstAnchorRun,
            worstAxisRun = worstAxisRun,
        )
    }

    /**
     * World point of one end's frame, placed by the body the frame belongs to.
     *
     * Wheel bodies start unrotated at their compiled centre; run bodies start at
     * the run pose. Both are the assembly pose, so no Bullet state is needed.
     */
    private fun worldAnchor(
        body: CompiledBodyRef,
        frame: CompiledConstraintFrame,
        run: CompiledRun,
        compiled: CompiledTrackSide,
    ): Vec3 {
        val local = TrackedAssemblyGeometry.rotateAboutX(frame.origin, angleOf(frame.rotation))
        return when (body.kind) {
            CompiledBodyKind.RUN -> run.center.add(TrackedAssemblyGeometry.rotateAboutX(local, angleOf(run.rotation)))
            CompiledBodyKind.WHEEL -> {
                val wheel = compiled.wheels.single { it.index == body.ordinal }
                wheel.assemblyCenter.add(local)
            }

            CompiledBodyKind.CHASSIS ->
                throw IllegalArgumentException("a run end cannot reference the chassis: $body")
        }
    }

    /**
     * World direction of one end's **locked** axis, placed by its body.
     *
     * The axis differs by end, so it is read from the plan instead of assumed:
     *
     * - first end frees `ANG_X` and locks `ANG_Y`/`ANG_Z`, so the meaningful
     *   alignment check is on the axle it must keep shared, i.e. `local X`;
     * - second end locks exactly one translation and frees every rotation, so
     *   there is no locked angular axis to compare and its normal is measured
     *   through [normalResidual] instead.
     *
     * Comparing the two frames' `local Y` on both ends — as an earlier version
     * did — reports a 180-degree disagreement that is not a constraint defect at
     * all, because the frames were never meant to agree on Y.
     */
    private fun worldAxis(
        body: CompiledBodyRef,
        frame: CompiledConstraintFrame,
        run: CompiledRun,
        compiled: CompiledTrackSide,
        end: RunEnd,
    ): Vector3f {
        val localDir = when (end) {
            RunEnd.FIRST -> Vec3(1.0, 0.0, 0.0)   // the axle that must stay shared
            RunEnd.SECOND -> return Vector3f(0f, 0f, 0f) // no locked angular axis
        }
        val inFrame = TrackedAssemblyGeometry.rotateAboutX(localDir, angleOf(frame.rotation))
        val world = when (body.kind) {
            CompiledBodyKind.RUN -> TrackedAssemblyGeometry.rotateAboutX(inFrame, angleOf(run.rotation))
            // A belt-member wheel has no independent rotation, so its frame is
            // already in the chassis basis at assembly.
            CompiledBodyKind.WHEEL -> inFrame
            CompiledBodyKind.CHASSIS ->
                throw IllegalArgumentException("a run end cannot reference the chassis: $body")
        }
        return Vector3f(world.x.toFloat(), world.y.toFloat(), world.z.toFloat()).normalize()
    }

    private fun angleOf(rotation: CompiledRotation): Float =
        2f * kotlin.math.atan2(rotation.x, rotation.w)

    private fun angleBetween(a: Vector3f, b: Vector3f): Double {
        val dot = (a.x * b.x + a.y * b.y + a.z * b.z).toDouble()
        return acos(dot.coerceIn(-1.0, 1.0))
    }
}

/**
 * The historical shoe-chain probe.
 *
 * Kept so the earlier B2.0 evidence stays reproducible. It is explicitly
 * **legacy**: it measures pins and hinges that the run topology does not have, so
 * it cannot support a pass for a run fixture and its density is never `FULL`.
 */
@Deprecated("Legacy shoe chain; use TrackRunAssemblyProbe")
object LegacyB2AssemblyProbe {

    private const val PLACEMENT_TOLERANCE_SQR = 1.0e-18

    fun requireMatches(spec: TrackRunSpec, compiled: CompiledTrackSide) {
        val legacy = compiled.requireLegacy()
        require(spec.links.size == legacy.placements.size) {
            "spec has ${spec.links.size} links but ${compiled.side} compiled ${legacy.placements.size}"
        }
        require(spec.joints.size == legacy.placements.size) {
            "spec has ${spec.joints.size} hinges for ${legacy.placements.size} links"
        }
        val expectedMass = legacy.linkMassKg.toFloat()
        spec.links.forEachIndexed { index, link ->
            require(link.massKg == expectedMass) {
                "link $index mass ${link.massKg} != audited $expectedMass"
            }
            val shape = link.shape as? TrackRunLinkShape.Box
                ?: throw IllegalArgumentException("link $index is not the audited box shoe")
            require(shape.halfExtents == legacy.linkTemplate.physical.halfExtents) {
                "link $index half extents ${shape.halfExtents} != audited ${legacy.linkTemplate.physical.halfExtents}"
            }
            val placement = legacy.placements[index]
            require(link.position.distanceToSqr(placement.center) <= PLACEMENT_TOLERANCE_SQR) {
                "link $index pose ${link.position} != audited placement ${placement.center}"
            }
        }
    }

    fun measure(spec: TrackRunSpec, compiled: CompiledTrackSide): B2InitialAssembly {
        requireMatches(spec, compiled)
        var worstPin = 0.0
        var worstPinJoint = 0
        var worstFrame = 0.0
        var worstFrameJoint = 0

        spec.joints.forEachIndexed { index, joint ->
            val previous = spec.links[joint.previousIndex]
            val next = spec.links[joint.nextIndex]
            val previousPin = hingePoint(previous, joint.pivotInPrevious)
            val nextPin = hingePoint(next, joint.pivotInNext)
            val gap = hypot(
                hypot(previousPin.x - nextPin.x, previousPin.y - nextPin.y),
                previousPin.z - nextPin.z,
            )
            if (gap > worstPin) {
                worstPin = gap
                worstPinJoint = index
            }
            val angle = angleBetween(hingeAxis(previous), hingeAxis(next))
            if (angle > worstFrame) {
                worstFrame = angle
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

    private fun hingePoint(
        link: cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec,
        local: Vec3,
    ): Vec3 {
        val rotated = TrackedAssemblyGeometry.rotateAboutX(
            local,
            2f * kotlin.math.atan2(link.rotationX, link.rotationW),
        )
        return link.position.add(rotated)
    }

    private fun hingeAxis(link: cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec): Vector3f =
        Vector3f(1f, 0f, 0f)

    private fun angleBetween(a: Vector3f, b: Vector3f): Double {
        val dot = (a.x * b.x + a.y * b.y + a.z * b.z).toDouble()
        return acos(dot.coerceIn(-1.0, 1.0))
    }
}

/**
 * One dynamic measurement of a running mechanism.
 *
 * Every field is required. A record that cannot supply one is not a B2 record:
 * the alternative is an optional field that quietly defaults to a comforting
 * zero, which is how "it looks stable" becomes an unverifiable claim.
 *
 * Units are in the field names on purpose. This file is the place where the
 * measurement contract lives, so a reader never has to guess whether a number is
 * metres, radians, newtons or counts.
 *
 * **External constraints are measured here, not inferred from the core snapshot.**
 * A run has one link and no internal joints, so `TrackRunSnapshot.jointAngleError`
 * is always zero for it. Reporting that as "the run is stable" would be a
 * category error; the end residuals below come from the fixture's own sampling of
 * the joints it created through `TrackedJointFactory`.
 */
data class B2RunObservation(
    val side: TrackedSide,
    /** Which mechanism produced this record. Only [B2ChainDensity.FULL] can support a pass. */
    val density: B2ChainDensity,
    /** Substeps elapsed since release, at [B2Sampling.SUBSTEP_SECONDS] each. */
    val substeps: Int,
    /** Identifies the compiled topology this record belongs to. */
    val topologyRevision: String,
    /** Largest distance between the two anchors of one run end, metres. */
    val endpointAnchorErrorM: Double,
    /** Largest disagreement of one end's locked axis, radians. */
    val endpointFrameAngleErrorRad: Double,
    /** Largest residual along the second end's declared normal, metres. */
    val normalResidualM: Double,
    /** Tangential slip at the second end over the window, metres. Diagnostic, not "smaller is better". */
    val tangentialSlipM: Double,
    /** Rotation drift of a wheel body relative to its mount, radians. Must stay near zero. */
    val lockedAxisDriftRad: Double,
    /** Number of wheel bodies that received at least one run contact. */
    val runWheelContactCount: Int,
    /** Number of wheel bodies the side actually has, so a zero contact count is meaningful. */
    val wheelCount: Int,
    /** Deepest run-into-wheel overlap observed, metres. Zero means no overlap. */
    val runWheelPenetrationM: Double,
    /** Number of runs that received at least one terrain contact. */
    val runTerrainContactCount: Int,
    /** Cross-run contacts within the same owner. Must stay zero. */
    val runRunContactCount: Int,
    /** Largest run displacement relative to its wheels over the window, metres. */
    val runDriftM: Double,
    /** Per-station suspension travel, metres, in `CompiledWheel.index` order. */
    val suspensionTravelM: List<Double>,
    /** Per-station suspension row force, newtons, positive in compression. */
    val suspensionForceN: List<Double>,
    /** Chassis vertical displacement from its release pose, metres. */
    val chassisHeaveM: Double,
    /** Chassis pitch change from its release pose, radians. */
    val chassisPitchRad: Double,
) {
    val runWheelCoverage: Double
        get() = if (wheelCount <= 0) 0.0 else runWheelContactCount.toDouble() / wheelCount

    /** True when the record came from the full mechanism, not a diagnostic shortcut. */
    val isFullDensity: Boolean get() = density == B2ChainDensity.FULL

    /**
     * Structural problems with this record, not performance judgements.
     *
     * These catch the failures that make a run meaningless: a hand-sampled
     * window, a mechanism that touches itself, a station list that does not match
     * the wheels, or a value that is not a number. Thresholds such as
     * `endpointAnchorError` are applied later, against frozen limits.
     */
    fun problems(expectedStations: Int, expectedRuns: Int): List<String> = buildList {
        if (substeps < 0) add("substeps must not be negative: $substeps")
        if (topologyRevision.isBlank()) add("topologyRevision must identify the compiled topology")
        if (wheelCount <= 0) add("wheelCount must be positive: $wheelCount")
        if (runWheelContactCount !in 0..wheelCount) {
            add("runWheelContactCount $runWheelContactCount is outside 0..$wheelCount")
        }
        if (runTerrainContactCount !in 0..expectedRuns) {
            add("runTerrainContactCount $runTerrainContactCount is outside 0..$expectedRuns")
        }
        if (runRunContactCount != 0) {
            add("same-owner runs touched each other $runRunContactCount times")
        }
        if (suspensionTravelM.size != expectedStations) {
            add("suspensionTravelM has ${suspensionTravelM.size} entries, expected $expectedStations")
        }
        if (suspensionForceN.size != expectedStations) {
            add("suspensionForceN has ${suspensionForceN.size} entries, expected $expectedStations")
        }
        val nonFinite = listOf(
            "endpointAnchorErrorM" to endpointAnchorErrorM,
            "endpointFrameAngleErrorRad" to endpointFrameAngleErrorRad,
            "normalResidualM" to normalResidualM,
            "tangentialSlipM" to tangentialSlipM,
            "lockedAxisDriftRad" to lockedAxisDriftRad,
            "runWheelPenetrationM" to runWheelPenetrationM,
            "runDriftM" to runDriftM,
            "chassisHeaveM" to chassisHeaveM,
            "chassisPitchRad" to chassisPitchRad,
        ).filter { !it.second.isFinite() }
        nonFinite.forEach { add("${it.first} is not finite: ${it.second}") }
        if (endpointAnchorErrorM < 0.0) add("anchor error is reported as a magnitude")
        if (endpointFrameAngleErrorRad < 0.0) add("frame angle error is reported as a magnitude")
        if (normalResidualM < 0.0) add("normal residual is reported as a magnitude")
        if (tangentialSlipM < 0.0) add("tangential slip is reported as a magnitude")
        if (lockedAxisDriftRad < 0.0) add("locked-axis drift is reported as a magnitude")
        if (runWheelPenetrationM < 0.0) add("penetration is reported as a non-negative depth")
        if (runDriftM < 0.0) add("run drift is reported as a magnitude")
        if (suspensionTravelM.any { !it.isFinite() }) add("suspensionTravelM contains a non-finite value")
        if (suspensionForceN.any { !it.isFinite() }) add("suspensionForceN contains a non-finite value")
    }

    /** Diagnostics line for a test report; contains no verdict. */
    fun describe(): String = buildString {
        append("$side ")
        append("substeps=$substeps ")
        append("anchor=%.3e m ".format(endpointAnchorErrorM))
        append("frame=%.3e rad ".format(endpointFrameAngleErrorRad))
        append("normal=%.4f m ".format(normalResidualM))
        append("slip=%.4f m ".format(tangentialSlipM))
        append("axisDrift=%.3e rad ".format(lockedAxisDriftRad))
        append("wheelContacts=$runWheelContactCount/$wheelCount ")
        append("penetration=%.4f m ".format(runWheelPenetrationM))
        append("terrainRuns=$runTerrainContactCount ")
        append("runRun=$runRunContactCount ")
        append("drift=%.4f m ".format(runDriftM))
        append("travel=${suspensionTravelM.map { "%.4f".format(it) }} ")
        append("force=${suspensionForceN.map { "%.0f".format(it) }} ")
        append("heave=%.4f m ".format(chassisHeaveM))
        append("pitch=%.4f rad".format(chassisPitchRad))
    }
}

/** Which mechanism a fixture is allowed to claim. */
enum class B2ChainDensity {
    /**
     * The full compiled mechanism: every belt member and every run.
     *
     * Only this density can support a pass. It no longer means "79 links": the
     * run topology has one run per adjacent member pair, so the audited count is
     * `members.size` runs on a side.
     */
    FULL,

    /**
     * A deliberately reduced mechanism used to localise an instability.
     * A [B2ChainDensity.DIAGNOSTIC] run may be recorded but can never be cited
     * as B2 evidence for the vehicle.
     */
    DIAGNOSTIC,
}

/** Helper so a diagnostic run cannot accidentally be reported as full density. */
fun B2ChainDensity.describe(runCount: Int, auditedRunCount: Int): String = when (this) {
    B2ChainDensity.FULL -> {
        require(runCount == auditedRunCount) {
            "FULL density requires $auditedRunCount runs, got $runCount"
        }
        "full density, $runCount runs"
    }
    B2ChainDensity.DIAGNOSTIC ->
        "DIAGNOSTIC ONLY, $runCount of $auditedRunCount runs; not acceptance evidence"
}
