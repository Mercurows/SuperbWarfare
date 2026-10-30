package com.atsuishio.superbwarfare.physics.tracked.legacy

import com.atsuishio.superbwarfare.physics.tracked.BedrockModelGeometry
import com.atsuishio.superbwarfare.physics.tracked.ClosedTrackLoopSolver
import com.atsuishio.superbwarfare.physics.tracked.CompiledGeometryAudit
import com.atsuishio.superbwarfare.physics.tracked.CompiledLinkCube
import com.atsuishio.superbwarfare.physics.tracked.CompiledLinkPlacement
import com.atsuishio.superbwarfare.physics.tracked.CompiledLinkTemplate
import com.atsuishio.superbwarfare.physics.tracked.CompiledPhysicalLinkShape
import com.atsuishio.superbwarfare.physics.tracked.CompiledRotation
import com.atsuishio.superbwarfare.physics.tracked.CompiledWheel
import com.atsuishio.superbwarfare.physics.tracked.LegacyShoeChainAudit
import com.atsuishio.superbwarfare.physics.tracked.TrackSideDefinition
import com.atsuishio.superbwarfare.physics.tracked.TrackedAssemblyGeometry
import com.atsuishio.superbwarfare.physics.tracked.TrackedGeometryAudit
import com.atsuishio.superbwarfare.physics.tracked.TrackedSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import com.atsuishio.superbwarfare.physics.tracked.TrackWheelRole
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * The historical 79-shoe closed chain, kept as an explicit legacy capability.
 *
 * This is **not** the physical topology any more. The run topology is compiled by
 * `TrackedVehicleCompiler`; this class exists so the earlier S1 and B2.0 evidence
 * stays reproducible and so an existing audit can be re-read. It must never
 * gate, feed or influence the run path.
 *
 * A failure here is reported as a warning, not an error: a belt whose shoe chain
 * cannot close is still a valid wheel-pair mechanism.
 */
object LegacyShoeChainCompiler {

    /**
     * Compile the shoe chain for [side].
     *
     * Returns null when the chain cannot be assembled; the caller decides how to
     * report that. It never mutates [wheels].
     */
    fun compile(
        side: TrackedSide,
        sideDefinition: TrackSideDefinition,
        definition: TrackedVehicleDefinition,
        geometry: BedrockModelGeometry,
        wheels: List<CompiledWheel>,
        warn: (String, String) -> Unit,
        info: (String, String) -> Unit,
    ): LegacyShoeChainAudit? {
        val label = "${side.name}.legacy"
        val template = compileLinkTemplate(
            sideDefinition.linkTemplateBone,
            geometry,
            definition,
            label,
            warn,
        ) ?: return null

        val driveIndex = wheels.indexOfFirst { it.role == TrackWheelRole.DRIVE_GUIDE }
        val idlerIndex = wheels.indexOfFirst { it.role == TrackWheelRole.IDLER_GUIDE }
        if (driveIndex < 0 || idlerIndex < 0) return null
        val idlerTensioner = wheels[idlerIndex].tensioner
        if (idlerTensioner == null) {
            warn(
                "$label.idler-fixed",
                "$label idler is fixed; the historical shoe chain is not used and the idler is not moved",
            )
            return null
        }
        if (!isLongitudinal(idlerTensioner.axis)) return null

        val circles = wheels.map { wheel ->
            ClosedTrackLoopSolver.Circle(
                z = wheel.bindCenter.z,
                y = wheel.bindCenter.y,
                pinRadius = ClosedTrackLoopSolver.pinLocusRadius(
                    wheelRadius = wheel.radius,
                    pinInset = definition.track.pinInset,
                    clearance = definition.track.clearance,
                    pitch = definition.track.pitch,
                ),
                wheelRadius = wheel.radius,
            )
        }

        val declared = definition.track.linkCount
        val loop = if (declared > 0) {
            ClosedTrackLoopSolver.solve(
                circles,
                definition.track.pitch,
                declared,
                idlerIndex,
                idlerTensioner.travel,
            )
        } else {
            ClosedTrackLoopSolver.candidateCounts(circles, definition.track.pitch)
                .firstNotNullOfOrNull { count ->
                    ClosedTrackLoopSolver.solve(
                        circles,
                        definition.track.pitch,
                        count,
                        idlerIndex,
                        idlerTensioner.travel,
                    )
                }
        } ?: run {
            warn(
                "$label.loop",
                "$label shoe chain does not close at ${definition.track.pitch} m; the run topology is unaffected",
            )
            return null
        }

        val placements = TrackedAssemblyGeometry.place(
            pins = loop.pins,
            centerX = template.centerX,
            boxCenterInPinFrame = template.physical.boxCenterInPinFrame,
            halfExtents = template.physical.halfExtents,
        )
        val audit = TrackedGeometryAudit.audit(
            placements = placements,
            wheels = wheels,
            shoeHalfWidth = template.width * 0.5,
            pinChordClearanceM = loop.minPinChordClearanceM,
        )
        if (audit.interpenetrates) {
            warn(
                "$label.shoe-interference",
                "$label link ${audit.worstLinkIndex} overlaps wheel ${audit.worstWheelBone} by " +
                    "${"%.2f".format(-audit.minShoeWheelGapM * 1000.0)} mm at assembly. " +
                    "This is the historical shoe chain; the run topology is unaffected",
            )
        } else if (audit.minShoeWheelGapM < CompiledGeometryAudit.INTERPENETRATION_TOLERANCE_M) {
            warn(
                "$label.shoe-contact",
                "$label shoe touches wheel ${audit.worstWheelBone} at link ${audit.worstLinkIndex}: " +
                    "gap ${"%.2f".format(audit.minShoeWheelGapM * 1000.0)} mm",
            )
        }
        val travelUsed = abs(loop.idlerOffset) / idlerTensioner.travel
        if (travelUsed > 0.9) {
            warn(
                "$label.tensioner.margin",
                "$label closes at ${"%.1f".format(loop.idlerOffset * 1000.0)} mm of a " +
                    "${"%.0f".format(idlerTensioner.travel * 1000.0)} mm tensioner travel; only " +
                    "${"%.1f".format((1.0 - travelUsed) * 100.0)}% take-up remains",
            )
        }
        if (!loop.valid) {
            warn(
                "$label.loop.error",
                "$label closure=${"%.3e".format(loop.closureError)} m " +
                    "pitch error=${"%.3e".format(loop.maxPitchError)} m",
            )
        }
        if (loop.maxJointAngleDegrees > definition.track.angularLimitDegrees + 1.0e-6) {
            warn(
                "$label.joint-angle",
                "$label demands ${"%.1f".format(loop.maxJointAngleDegrees)}° between links but " +
                    "AngularLimitDegrees is ${definition.track.angularLimitDegrees}°",
            )
        }
        info(
            "$label.summary",
            "$label: ${loop.linkCount} links, idler offset " +
                "${"%.1f".format(loop.idlerOffset * 1000.0)} mm, shoe gap " +
                "${"%.2f".format(audit.minShoeWheelGapM * 1000.0)} mm",
        )
        return LegacyShoeChainAudit(
            linkTemplate = template,
            loop = loop,
            driveIndex = driveIndex,
            idlerIndex = idlerIndex,
            linkMassKg = definition.track.linkMass,
            placements = placements,
            geometry = audit,
        )
    }

    /** The legacy shoe shape, with the single-plain-cube requirement it needs. */
    private fun compileLinkTemplate(
        boneName: String,
        geometry: BedrockModelGeometry,
        definition: TrackedVehicleDefinition,
        label: String,
        warn: (String, String) -> Unit,
    ): CompiledLinkTemplate? {
        val bone = geometry.bones[boneName] ?: return null
        if (bone.cubes.isEmpty()) return null

        val local = ArrayList<Vector3d>()
        for (cube in bone.cubes) local += geometry.localCubeCorners(cube)
        if (local.isEmpty()) return null

        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (point in local) {
            minX = minOf(minX, point.x); maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y); maxY = maxOf(maxY, point.y)
            minZ = minOf(minZ, point.z); maxZ = maxOf(maxZ, point.z)
        }
        val scale = definition.renderScale / 16.0
        val width = (maxX - minX) * scale
        val thickness = (maxY - minY) * scale
        val length = (maxZ - minZ) * scale
        if (!(width > 0.0) || !(thickness > 0.0) || !(length > 0.0)) return null

        val pinInset = definition.track.pinInset
        val shiftY = -minY - pinInset / scale
        val shiftZ = -(minZ + maxZ) * 0.5

        val cubes = bone.cubes.map { cube ->
            val centre = geometry.localCubeCentre(cube)
            CompiledLinkCube(
                center = Vec3(
                    (centre.x - (minX + maxX) * 0.5) * scale,
                    (centre.y + shiftY) * scale,
                    (centre.z + shiftZ) * scale,
                ),
                half = Vec3(cube.size.x * scale * 0.5, cube.size.y * scale * 0.5, cube.size.z * scale * 0.5),
                rotation = cube.rotation?.let {
                    CompiledRotation(
                        x = Math.toRadians(it.x).toFloat(),
                        y = Math.toRadians(it.y).toFloat(),
                        z = Math.toRadians(it.z).toFloat(),
                    )
                } ?: CompiledRotation.IDENTITY,
            )
        }

        val boxCenter = Vec3(
            0.0,
            ((minY + maxY) * 0.5 + shiftY) * scale,
            ((minZ + maxZ) * 0.5 + shiftZ) * scale,
        )
        val simple = bone.cubes.size == 1 && bone.cubes.first().rotation == null && bone.cubes.first().inflate == 0.0
        if (!simple) {
            warn(
                "$label.template.shape",
                "$label legacy shoe template $boneName has ${bone.cubes.size} cube(s); " +
                    "its physical shoe is an AABB approximation. The run topology is unaffected",
            )
        }

        return CompiledLinkTemplate(
            bone = boneName,
            cubes = cubes,
            width = width,
            thickness = thickness,
            length = length,
            centerX = geometry.toPhysicsLocal(geometry.worldPivot(boneName), definition.renderScale).x -
                definition.chassisOrigin.x,
            physical = CompiledPhysicalLinkShape(
                halfExtents = Vec3(width * 0.5, thickness * 0.5, length * 0.5),
                boxCenterInPinFrame = boxCenter,
            ),
        )
    }

    private fun isLongitudinal(axis: Vec3): Boolean =
        abs(abs(axis.z) - 1.0) < 1.0e-6 && abs(axis.x) < 1.0e-6 && abs(axis.y) < 1.0e-6

    /** Largest wheel-to-shoe overlap, exposed for a historical report. */
    fun worstOverlap(audit: CompiledGeometryAudit): Double = max(0.0, -audit.minShoeWheelGapM)

    /** Distance helper kept so the legacy report can quote a pin chord. */
    fun pinChord(loop: ClosedTrackLoopSolver.Result): Double =
        hypot(loop.pitch, loop.minPinChordClearanceM)
}
