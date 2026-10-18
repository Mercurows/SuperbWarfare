package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

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
    /** Local axis of free travel, metres. */
    val axis: Vec3,
)

data class CompiledWheel(
    val side: TrackedSide,
    val role: TrackWheelRole,
    val bone: String,
    /** Stable ordinal inside its side, in longitudinal order. */
    val index: Int,
    val center: Vec3,
    val radius: Double,
    /** Lateral half width, metres. */
    val halfWidth: Double,
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
    val rotation: Quaternionf = Quaternionf(),
)

data class CompiledLinkTemplate(
    val bone: String,
    val cubes: List<CompiledLinkCube>,
    val width: Double,
    val thickness: Double,
    val length: Double,
)

data class CompiledTrackSide(
    val side: TrackedSide,
    val wheels: List<CompiledWheel>,
    val linkTemplate: CompiledLinkTemplate,
    val loop: ClosedTrackLoopSolver.Result,
    val driveIndex: Int,
    val idlerIndex: Int,
    /** Mass of one shoe, kg. */
    val linkMassKg: Double,
) {
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

/**
 * Immutable result of compiling one vehicle definition against one model.
 * It holds no Minecraft or Bullet object and can be produced on any thread.
 */
data class TrackedVehicleSpec(
    val vehicleId: String,
    val model: String,
    val modelSha256: String,
    val renderScale: Double,
    val massKg: Double,
    val pitch: Double,
    val pinInset: Double,
    val linkMassKg: Double,
    val angularLimitDegrees: Double,
    val drivetrain: TrackDrivetrainDefinition,
    val sides: Map<TrackedSide, CompiledTrackSide>,
    val audit: List<TrackedAuditFinding>,
) {
    val hasErrors: Boolean get() = audit.any { it.severity == AuditSeverity.ERROR }

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
