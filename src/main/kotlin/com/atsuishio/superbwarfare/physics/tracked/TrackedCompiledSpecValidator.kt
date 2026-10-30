package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import kotlin.math.abs

/**
 * Last gate before a compiled side is declared usable.
 *
 * The compiler proves the authored mechanism. This proves that the *result* can
 * actually be handed to the core: every number is finite and representable as a
 * `Float`, every joint frame is a real rotation, every reference resolves, and
 * every run the adapter will build is a valid `TrackRunSpec`. A definition that
 * compiles but overflows in the adapter is a defect, not a runtime surprise.
 */
object TrackedCompiledSpecValidator {

    /** Largest magnitude a `Float` can hold while still behaving like a number. */
    private const val FLOAT_LIMIT = 3.0e38

    fun validate(spec: TrackedVehicleSpec): List<TrackedAuditFinding> {
        val findings = ArrayList<TrackedAuditFinding>()
        fun error(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.ERROR, code, message))

        if (!spec.mass.closes()) {
            error("mass-closure", "sprung assembly + running gear != ${spec.mass.totalMassKg} kg")
        }
        if (!spec.mass.detailCloses()) {
            error(
                "mass-detail",
                "wheel ${"%.3f".format(spec.mass.wheels.sumOf { it.massKg })} + run " +
                    "${"%.3f".format(spec.mass.runs.sumOf { it.massKg })} kg does not equal running gear " +
                    "${"%.3f".format(spec.mass.runningGearMassKg)} kg",
            )
        }

        for (side in TrackedSide.entries) {
            val compiled = spec.sides[side]
            if (compiled == null) {
                error("side.missing", "$side was not compiled")
                continue
            }
            validateSide(spec, compiled, ::error)
        }
        return findings
    }

    private fun validateSide(
        spec: TrackedVehicleSpec,
        side: CompiledTrackSide,
        error: (String, String) -> Unit,
    ) {
        val label = side.side.name
        if (side.wheels.isEmpty()) error("$label.wheels", "$label has no wheels")
        if (side.members.size < 2) error("$label.members", "$label belt has ${side.members.size} member(s)")
        if (side.runs.size != side.members.size) {
            error("$label.runs", "$label has ${side.runs.size} runs for ${side.members.size} members")
        }
        if (side.mounts.size != side.wheels.size) {
            error("$label.mounts", "$label has ${side.mounts.size} mounts for ${side.wheels.size} wheels")
        }

        val wheelIndices = side.wheels.map { it.index }.toSet()
        side.members.forEachIndexed { ordinal, member ->
            if (member.ordinal != ordinal) {
                error("$label.member.ordinal", "$label member $ordinal has ordinal ${member.ordinal}")
            }
            if (member.wheelIndex !in wheelIndices) {
                error("$label.member.ref", "$label member ${member.bone} references unknown wheel ${member.wheelIndex}")
            }
        }
        if (side.members.map { it.wheelIndex }.toSet() != wheelIndices) {
            error("$label.member.coverage", "$label belt members do not cover every wheel exactly once")
        }

        for (run in side.runs) {
            validateRun(label, run, side, spec, error)
        }

        for (mount in side.mounts) {
            validateMount(label, mount, side, error)
        }

        // The adapter must accept every run; if it does not, the compile result is
        // not usable even though the authored mechanism looked valid.
        val specs = runCatching { TrackedRunSpecs.compileRuns(spec, side.side) }
        specs.exceptionOrNull()?.let {
            error("$label.adapter", "$label run adapter rejected the compiled runs: ${it.message}")
            return
        }
        specs.getOrNull()?.forEach { runSpec ->
            val problems = runSpec.problems()
            if (problems.isNotEmpty()) {
                error("$label.adapter", "$label run '${runSpec.name}' is not a valid spec: $problems")
            }
        }
    }

    private fun validateRun(
        label: String,
        run: CompiledRun,
        side: CompiledTrackSide,
        spec: TrackedVehicleSpec,
        error: (String, String) -> Unit,
    ) {
        val code = "$label.run.${run.index}"
        if (!run.massKg.isFinite() || run.massKg <= 0.0) error(code, "run mass ${run.massKg} kg is not positive")
        if (!run.massKg.isFloatRepresentable()) error(code, "run mass ${run.massKg} kg is not representable as Float")
        if (!run.center.isFiniteVector()) error(code, "run centre is not finite: ${run.center}")
        if (!run.halfExtents.isFiniteVector()) error(code, "run extents are not finite: ${run.halfExtents}")
        if (run.halfExtents.x <= 0.0 || run.halfExtents.y <= 0.0 || run.halfExtents.z <= 0.0) {
            error(code, "run extents must be positive: ${run.halfExtents}")
        }
        if (!run.center.isFloatRepresentable() || !run.halfExtents.isFloatRepresentable()) {
            error(code, "run geometry is not representable as Float")
        }
        if (!run.rotation.isUnit()) error(code, "run rotation is not a unit quaternion: ${run.rotation}")

        validateAttachment(label, run, run.first, RunEnd.FIRST, error)
        validateAttachment(label, run, run.second, RunEnd.SECOND, error)

        if (run.firstWheelIndex !in side.wheels.indices && side.wheels.none { it.index == run.firstWheelIndex }) {
            error(code, "run first wheel ${run.firstWheelIndex} does not resolve")
        }
        if (side.wheels.none { it.index == run.secondWheelIndex }) {
            error(code, "run second wheel ${run.secondWheelIndex} does not resolve")
        }
    }

    private fun validateAttachment(
        label: String,
        run: CompiledRun,
        attachment: CompiledRunAttachment,
        end: RunEnd,
        error: (String, String) -> Unit,
    ) {
        val code = "$label.run.${run.index}.${end.name.lowercase()}"
        if (attachment.end != end) error(code, "attachment end ${attachment.end} is not $end")
        if (!attachment.anchorInRun.isFiniteVector()) error(code, "run anchor is not finite")
        if (!attachment.anchorInWheel.isFiniteVector()) error(code, "wheel anchor is not finite")
        if (!attachment.anchorInRun.isFloatRepresentable() || !attachment.anchorInWheel.isFloatRepresentable()) {
            error(code, "attachment anchors are not representable as Float")
        }
        if (!attachment.frameInRun.isUnit()) error(code, "run frame is not a unit quaternion")
        if (!attachment.frameInWheel.isUnit()) error(code, "wheel frame is not a unit quaternion")

        val plan = attachment.joint
        if (!plan.frameInA.origin.isFiniteVector() || !plan.frameInB.origin.isFiniteVector()) {
            error(code, "joint frame origins are not finite")
        }
        if (!plan.frameInA.rotation.isUnit() || !plan.frameInB.rotation.isUnit()) {
            error(code, "joint frame rotations are not unit quaternions")
        }
        when (end) {
            RunEnd.FIRST -> {
                if (!plan.isFirstRunEnd) {
                    error(code, "first end must lock all translation and free only the axle rotation")
                }
                if (plan.bodyA.kind != CompiledBodyKind.WHEEL || plan.bodyB.kind != CompiledBodyKind.RUN) {
                    error(code, "first end must be wheel -> run, was ${plan.bodyA} -> ${plan.bodyB}")
                }
            }

            RunEnd.SECOND -> {
                if (!plan.isSecondRunEnd) {
                    error(code, "second end must lock exactly one translation and free every rotation")
                }
                if (plan.bodyA.kind != CompiledBodyKind.RUN || plan.bodyB.kind != CompiledBodyKind.WHEEL) {
                    error(code, "second end must be run -> wheel, was ${plan.bodyA} -> ${plan.bodyB}")
                }
                val normal = attachment.lockedNormalLocal
                if (normal == null) {
                    error(code, "second end declares no locked normal")
                } else if (abs(normal.length() - 1.0) > 1.0e-6) {
                    error(code, "locked normal is not a unit vector: $normal")
                }
            }
        }
    }

    private fun validateMount(
        label: String,
        mount: CompiledWheelMount,
        side: CompiledTrackSide,
        error: (String, String) -> Unit,
    ) {
        val code = "$label.mount.${mount.bone}"
        if (side.wheels.none { it.index == mount.wheelIndex }) {
            error(code, "mount references unknown wheel ${mount.wheelIndex}")
        }
        if (!mount.axis.isFiniteVector()) error(code, "mount axis is not finite")
        if (!mount.locksAllRotation) error(code, "mount ${mount.bone} leaves a rotation free")
        if (mount.kind == WheelMountKind.FIXED && mount.joint?.freeLinearAxes?.isNotEmpty() == true) {
            error(code, "fixed mount ${mount.bone} leaves a translation free")
        }
        if (mount.kind == WheelMountKind.PRISMATIC) {
            if (mount.axis.length() < 1.0e-6) error(code, "prismatic mount ${mount.bone} has a zero axis")
            if (!(mount.limitUpM >= 0.0) || !(mount.limitDownM >= 0.0)) {
                error(code, "mount ${mount.bone} limits must be non-negative")
            }
            if (mount.joint == null) error(code, "prismatic mount ${mount.bone} has no joint plan")
        }
        mount.springRow?.let { row ->
            if (!row.isUsable) error(code, "spring row for ${mount.bone} is not usable")
        }
        if (mount.isTensioned && (mount.preloadN ?: 0.0) < 0.0) {
            error(code, "tensioner preload for ${mount.bone} is negative")
        }
    }

    private fun CompiledRotation.isUnit(): Boolean {
        val length = x.toDouble() * x + y.toDouble() * y + z.toDouble() * z + w.toDouble() * w
        return length.isFinite() && abs(length - 1.0) < 1.0e-5
    }

    private fun net.minecraft.world.phys.Vec3.isFloatRepresentable(): Boolean =
        isFiniteVector() &&
            abs(x) < FLOAT_LIMIT && abs(y) < FLOAT_LIMIT && abs(z) < FLOAT_LIMIT &&
            x.toFloat().isFinite() && y.toFloat().isFinite() && z.toFloat().isFinite()

    private fun Double.isFloatRepresentable(): Boolean =
        isFinite() && abs(this) < FLOAT_LIMIT && toFloat().isFinite()

    /** Shape validation that mirrors what the core spec will do. */
    fun validateShape(shape: TrackRunLinkShape): List<String> = when (shape) {
        is TrackRunLinkShape.Box -> if (shape.halfExtents.x > 0 && shape.halfExtents.y > 0 && shape.halfExtents.z > 0) {
            emptyList()
        } else {
            listOf("box extents must be positive: ${shape.halfExtents}")
        }

        is TrackRunLinkShape.Cylinder -> buildList {
            if (shape.radius <= 0f) add("cylinder radius must be positive")
            if (shape.halfHeight <= 0f) add("cylinder half height must be positive")
            if (shape.axis !in 0..2) add("cylinder axis must be 0..2")
        }
    }

    /** Convenience for tests: true when a spec carries no errors at all. */
    fun isClean(spec: TrackedVehicleSpec): Boolean = validate(spec).isEmpty()

    /** The core spec problems of every run on one side. */
    fun runSpecProblems(spec: TrackedVehicleSpec, side: TrackedSide): List<String> =
        TrackedRunSpecs.compileRuns(spec, side).flatMap { run: TrackRunSpec -> run.problems() }
}
