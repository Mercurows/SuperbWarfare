package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3d
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Turns a [TrackedVehicleDefinition] and its Bedrock asset into an immutable
 * [TrackedVehicleSpec], and reports every mechanism problem it can prove.
 *
 * The compiler is deliberately strict about *structure* (roles, bones, ranges)
 * and deliberately honest about *numbers*: it never silently rescales a wheel,
 * stretches a shoe or invents a missing pin. Values that cannot be satisfied are
 * reported as errors; values that merely need a human decision are warnings.
 */
private data class DraftWheel(
    val definition: TrackWheelDefinition,
    val index: Int,
    val center: Vec3,
    val radius: Double,
    val width: Double,
    val tensioner: CompiledTensioner?,
)

object TrackedVehicleCompiler {

    fun compile(
        vehicleId: String,
        definition: TrackedVehicleDefinition,
        modelJson: String,
    ): TrackedVehicleSpec {
        val findings = ArrayList<TrackedAuditFinding>()
        fun error(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.ERROR, code, message))
        fun warn(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.WARN, code, message))
        fun info(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.INFO, code, message))

        if (definition.model.isBlank()) error("model", "no model path")
        if (!definition.massKg.isFinite() || definition.massKg <= 100.0) {
            error("mass", "MassKg must be a positive vehicle mass, was ${definition.massKg}")
        }
        if (!definition.renderScale.isFinite() || definition.renderScale !in 0.25..4.0) {
            error("render-scale", "RenderScale must be within 0.25..4.0, was ${definition.renderScale}")
        }
        if (!definition.track.pitch.isFinite() || definition.track.pitch <= 0.001) {
            error("pitch", "Track.Pitch must be positive metres, was ${definition.track.pitch}")
        }
        if (definition.track.angularLimitDegrees !in 1.0..120.0) {
            error("angular-limit", "Track.AngularLimitDegrees out of range: ${definition.track.angularLimitDegrees}")
        }

        val geometry = try {
            BedrockModelGeometry.parse(modelJson)
        } catch (failure: Throwable) {
            error("model-parse", "cannot read geometry: ${failure.message}")
            return failed(vehicleId, definition, modelJson, findings)
        }

        val sides = LinkedHashMap<TrackedSide, CompiledTrackSide>()
        for (side in TrackedSide.entries) {
            val sideDefinition = definition.side(side)
            val compiled = compileSide(side, sideDefinition, definition, geometry, ::error, ::warn, ::info)
            if (compiled != null) sides[side] = compiled
        }

        val totalStationMass = sides.values.flatMap { it.wheels }.filter { it.isSuspended }
            .sumOf { it.suspension.stationMassKg }
        if (totalStationMass >= definition.massKg) {
            error(
                "mass-budget",
                "sprung stations carry ${"%.1f".format(totalStationMass)} kg of a ${"%.1f".format(definition.massKg)} kg vehicle; " +
                    "links and wheels also need mass",
            )
        }

        return TrackedVehicleSpec(
            vehicleId = vehicleId,
            model = definition.model,
            modelSha256 = sha256(modelJson),
            renderScale = definition.renderScale,
            massKg = definition.massKg,
            pitch = definition.track.pitch,
            pinInset = definition.track.pinInset,
            linkMassKg = definition.track.linkMass,
            angularLimitDegrees = definition.track.angularLimitDegrees,
            drivetrain = definition.drivetrain,
            sides = sides,
            audit = findings.toList(),
        )
    }

    private fun compileSide(
        side: TrackedSide,
        sideDefinition: TrackSideDefinition,
        definition: TrackedVehicleDefinition,
        geometry: BedrockModelGeometry,
        error: (String, String) -> Unit,
        warn: (String, String) -> Unit,
        info: (String, String) -> Unit,
    ): CompiledTrackSide? {
        val label = side.name
        if (sideDefinition.wheels.isEmpty()) {
            error("$label.empty", "$label side declares no wheels")
            return null
        }
        if (sideDefinition.linkTemplateBone.isBlank()) {
            error("$label.template", "$label side declares no LinkTemplateBone")
            return null
        }
        if (sideDefinition.linkTemplateBone !in geometry.bones) {
            error("$label.template.missing", "missing link template bone ${sideDefinition.linkTemplateBone}")
            return null
        }

        val linkTemplate = compileLinkTemplate(sideDefinition.linkTemplateBone, geometry, definition, label, error) ?: return null
        if (definition.track.pinInset < 0.0 || definition.track.pinInset > linkTemplate.thickness + 1.0e-6) {
            error(
                "$label.pin-inset",
                "PinInset ${definition.track.pinInset} m is outside the link thickness ${"%.4f".format(linkTemplate.thickness)} m",
            )
        }

        // Resolve every wheel from its explicit bone. Ordering is by authored
        // longitudinal position so a side's stable indices do not depend on the
        // order someone happened to write the JSON list in.
        data class Resolved(val definition: TrackWheelDefinition, val center: Vec3, val radius: Double, val width: Double)

        val resolved = ArrayList<Resolved>(sideDefinition.wheels.size)
        for (wheel in sideDefinition.wheels) {
            if (wheel.bone.isBlank()) {
                error("$label.wheel.bone", "$label wheel has no bone")
                continue
            }
            if (wheel.bone !in geometry.bones) {
                error("$label.wheel.missing", "missing wheel bone ${wheel.bone}")
                continue
            }
            if (wheel.role == TrackWheelRole.SUPPORT && (wheel.loadShare <= 0.0 || !wheel.loadShare.isFinite())) {
                error("$label.wheel.load", "${wheel.bone} has a non-positive LoadShare")
                continue
            }
            if (wheel.radius <= 0.01 || wheel.halfWidth <= 0.01) {
                error("$label.wheel.size", "${wheel.bone} needs explicit Radius and HalfWidth")
                continue
            }
            val pivotModel = geometry.worldPivot(wheel.bone)
            val rawPivot = geometry.toPhysicsLocal(pivotModel, definition.renderScale)
            val origin = definition.chassisOrigin
            val pivot = Vec3(rawPivot.x - origin.x, rawPivot.y - origin.y, rawPivot.z - origin.z)
            // Cross-check the authored radius against the mesh so a renamed or
            // re-scaled bone cannot silently leave a wheel the wrong size. The
            // mesh may legitimately be larger because it includes the arm, so
            // this is a diagnostic rather than a hard limit.
            var meshExtent = 0.0
            for (corner in geometry.subtreeCorners(wheel.bone)) {
                val local = geometry.toPhysicsLocal(corner, definition.renderScale)
                meshExtent = maxOf(meshExtent, hypot(local.y - pivot.y, local.z - pivot.z))
            }
            if (meshExtent > 0.0 && wheel.radius > meshExtent + 0.01) {
                warn(
                    "$label.wheel.radius",
                    "${wheel.bone} authored radius ${wheel.radius} m exceeds its mesh extent ${"%.3f".format(meshExtent)} m",
                )
            }
            resolved += Resolved(
                wheel,
                Vec3(pivot.x, pivot.y, pivot.z),
                wheel.radius,
                wheel.halfWidth,
            )
            if (wheel.role == TrackWheelRole.RETURN_ROLLER) {
                info("$label.return-roller", "${wheel.bone} is a fixed return roller and carries no ground load")
            }
        }

        if (resolved.isEmpty()) return null

        val driveCount = resolved.count { it.definition.role == TrackWheelRole.DRIVE_GUIDE }
        val idlerCount = resolved.count { it.definition.role == TrackWheelRole.IDLER_GUIDE }
        val supportCount = resolved.count { it.definition.role == TrackWheelRole.SUPPORT }
        if (driveCount != 1) error("$label.drive", "$label side needs exactly one DriveGuide, found $driveCount")
        if (idlerCount != 1) error("$label.idler", "$label side needs exactly one IdlerGuide, found $idlerCount")
        if (supportCount < 2) error("$label.support", "$label side needs at least two Support wheels, found $supportCount")

        val ordered = resolved.sortedWith(
            compareByDescending<Resolved> { it.center.z }.thenBy { it.definition.bone },
        )
        val draftWheels = ordered.mapIndexed { index, wheel ->
            DraftWheel(
                definition = wheel.definition,
                index = index,
                center = wheel.center,
                radius = wheel.radius,
                width = wheel.width,
                tensioner = wheel.definition.tensioner?.let {
                    CompiledTensioner(
                        travel = it.travel,
                        preloadN = it.preload,
                        dampingNsPerM = it.damping,
                        axis = Vec3(0.0, 0.0, 1.0),
                    )
                },
            )
        }

        val driveIndex = draftWheels.indexOfFirst { it.definition.role == TrackWheelRole.DRIVE_GUIDE }
        val idlerIndex = draftWheels.indexOfFirst { it.definition.role == TrackWheelRole.IDLER_GUIDE }
        if (driveIndex < 0 || idlerIndex < 0) return null

        val tensionerTravel = draftWheels.getOrNull(idlerIndex)?.tensioner?.travel ?: 0.10
        if (draftWheels.getOrNull(idlerIndex)?.tensioner == null) {
            warn("$label.tensioner", "$label idler has no tensioner block; using a ${tensionerTravel} m passive travel")
        }

        val circles = draftWheels.map { wheel ->
            ClosedTrackLoopSolver.Circle(
                z = wheel.center.z,
                y = wheel.center.y,
                pinRadius = wheel.radius + definition.track.pinInset + definition.track.clearance,
                wheelRadius = wheel.radius,
            )
        }

        val declared = definition.track.linkCount
        val loop = (if (declared > 0) {
            ClosedTrackLoopSolver.solve(circles, definition.track.pitch, declared, idlerIndex, tensionerTravel)
                ?: run {
                    val candidates = ClosedTrackLoopSolver.candidateCounts(circles, definition.track.pitch)
                    error(
                        "$label.loop",
                        "$label cannot close $declared links of ${definition.track.pitch} m within " +
                            "${tensionerTravel} m of idler travel; candidate counts $candidates",
                    )
                    null
                }
        } else {
            val candidates = ClosedTrackLoopSolver.candidateCounts(circles, definition.track.pitch)
            candidates.firstNotNullOfOrNull { count ->
                ClosedTrackLoopSolver.solve(circles, definition.track.pitch, count, idlerIndex, tensionerTravel)
            } ?: run {
                error("$label.loop", "$label has no closable link count near $candidates")
                null
            }
        }) ?: return null

        val linkMass = definition.track.linkMass
        if (!linkMass.isFinite() || linkMass <= 0.0) {
            error("$label.link-mass", "$label Track.LinkMass must be specified; a shoe is a real body")
        }
        draftWheels.filter { it.definition.mass <= 0.0 }.forEach {
            error("$label.wheel-mass", "${it.definition.bone} Mass must be specified; a wheel is a real body")
        }
        val gearMass = linkMass.coerceAtLeast(0.0) * loop.linkCount + draftWheels.sumOf { it.definition.mass.coerceAtLeast(0.0) }
        val sprungPerSide = definition.massKg * 0.5 - gearMass
        if (sprungPerSide <= 0.0) {
            error(
                "$label.mass-budget",
                "$label running gear is ${"%.0f".format(gearMass)} kg against a ${"%.0f".format(definition.massKg * 0.5)} kg half vehicle",
            )
        }
        val supportMass = draftWheels.filter { it.definition.role == TrackWheelRole.SUPPORT }
            .sumOf { it.definition.loadShare.coerceAtLeast(0.0) }
        val wheels = draftWheels.map { wheel ->
            val suspensionDefinition = wheel.definition.suspension?.applyTo(sideDefinition.suspension)
                ?: sideDefinition.suspension
            val share = if (wheel.definition.role == TrackWheelRole.SUPPORT && supportMass > 0.0) {
                sprungPerSide.coerceAtLeast(0.0) * wheel.definition.loadShare / supportMass
            } else {
                0.0
            }
            val suspension = compileSuspension(
                suspensionDefinition, share, definition, label, wheel.definition.bone,
                sprung = wheel.definition.role == TrackWheelRole.SUPPORT, error = error,
            )
            CompiledWheel(
                side = side,
                role = wheel.definition.role,
                bone = wheel.definition.bone,
                index = wheel.index,
                center = wheel.center,
                radius = wheel.radius,
                halfWidth = wheel.width,
                massKg = wheel.definition.mass,
                suspension = suspension,
                tensioner = wheel.tensioner,
            )
        }
        info(
            "$label.mass",
            "$label running gear ${"%.0f".format(gearMass)} kg, sprung ${"%.0f".format(sprungPerSide)} kg per side",
        )

        if (!loop.valid) {
            error(
                "$label.loop.error",
                "$label closure=${"%.3e".format(loop.closureError)} m pitch error=${"%.3e".format(loop.maxPitchError)} m",
            )
        }
        if (loop.maxJointAngleDegrees > definition.track.angularLimitDegrees + 1.0e-6) {
            error(
                "$label.joint-angle",
                "$label demands ${"%.1f".format(loop.maxJointAngleDegrees)}° between links but " +
                    "AngularLimitDegrees is ${definition.track.angularLimitDegrees}°",
            )
        }
        if (loop.minWheelClearance < -0.002) {
            warn(
                "$label.clearance",
                "$label pin polygon intrudes ${"%.2f".format(-loop.minWheelClearance * 1000.0)} mm into a wheel",
            )
        }
        val lengthError = abs(linkTemplate.length - definition.track.pitch)
        if (lengthError > definition.track.pitch * 0.05) {
            warn(
                "$label.shoe-length",
                "$label authored shoe is ${"%.4f".format(linkTemplate.length)} m long against a " +
                    "${"%.4f".format(definition.track.pitch)} m pitch; visible gaps or overlaps must be approved",
            )
        }
        info(
            "$label.summary",
            "$label: ${wheels.size} wheels, ${loop.linkCount} links, pitch ${definition.track.pitch} m, " +
                "length ${"%.4f".format(loop.totalLength)} m, idler offset ${"%.1f".format(loop.idlerOffset * 1000.0)} mm, " +
                "max wrap ${"%.1f".format(loop.maxJointAngleDegrees)}°",
        )

        return CompiledTrackSide(side, wheels, linkTemplate, loop, driveIndex, idlerIndex,
            linkMassKg = linkMass.coerceAtLeast(0.0))
    }

    private fun compileSuspension(
        definition: TrackSuspensionDefinition,
        stationMassKg: Double,
        vehicle: TrackedVehicleDefinition,
        label: String,
        bone: String,
        sprung: Boolean,
        error: (String, String) -> Unit,
    ): CompiledSuspension {
        val travelUp = definition.travelUp
        val travelDown = definition.travelDown
        if (!travelUp.isFinite() || travelUp <= 0.001 || !travelDown.isFinite() || travelDown <= 0.001) {
            error("$label.suspension", "$bone has invalid travel up=$travelUp down=$travelDown")
        }
        if (!definition.frequency.isFinite() || definition.frequency <= 0.05) {
            error("$label.frequency", "$bone has invalid frequency ${definition.frequency}")
        }
        val omega = 2.0 * PI * definition.frequency
        val stiffness = stationMassKg * omega * omega
        val critical = 2.0 * sqrt(stiffness * stationMassKg)
        return CompiledSuspension(
            travelUp = travelUp,
            travelDown = travelDown,
            frequency = definition.frequency,
            stiffnessNPerM = stiffness,
            compressionDampingNsPerM = critical * definition.compressionDampingRatio,
            reboundDampingNsPerM = critical * definition.reboundDampingRatio,
            maxForceN = definition.maxForce,
            bumpStopStart = definition.bumpStopStart,
            bumpStopExponent = definition.bumpStopExponent,
            stationMassKg = stationMassKg,
            staticCompressionM = if (omega > 1.0e-6) 9.81 / (omega * omega) else 0.0,
        ).also {
            if (sprung && stationMassKg > 0.0 && definition.maxForce < stationMassKg * 9.81) {
                error(
                    "$label.max-force",
                    "$bone MaxForce ${definition.maxForce} N cannot carry its ${"%.0f".format(stationMassKg)} kg share",
                )
            }
            if (sprung && stationMassKg <= 0.0) {
                error("$label.station-mass", "$bone is a support wheel but received no sprung mass")
            }
        }
    }

    private fun compileLinkTemplate(
        boneName: String,
        geometry: BedrockModelGeometry,
        definition: TrackedVehicleDefinition,
        label: String,
        error: (String, String) -> Unit,
    ): CompiledLinkTemplate? {
        val bone = geometry.bones[boneName] ?: return null
        if (bone.cubes.isEmpty()) {
            error("$label.template.cubes", "link template $boneName has no cubes")
            return null
        }
        // Template bounds in *bone local* space, so the pin frame follows the
        // authored shoe rather than the whole vehicle.
        val local = ArrayList<Vector3d>()
        for (cube in bone.cubes) local += geometry.localCubeCorners(cube)
        if (local.isEmpty()) {
            error("$label.template.cubes", "link template $boneName produced no corners")
            return null
        }
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
        val pinInset = definition.track.pinInset
        val shiftY = -minY - pinInset / scale
        val shiftZ = -(minZ + maxZ) * 0.5

        val cubes = bone.cubes.map { cube ->
            val centre = geometry.localCubeCentre(cube)
            val rotation = cube.rotation?.let {
                Quaternionf()
                    .rotateZ(Math.toRadians(it.z).toFloat())
                    .rotateY(Math.toRadians(it.y).toFloat())
                    .rotateX(Math.toRadians(it.x).toFloat())
            } ?: Quaternionf()
            CompiledLinkCube(
                center = Vec3(
                    (centre.x - (minX + maxX) * 0.5) * scale,
                    (centre.y + shiftY) * scale,
                    (centre.z + shiftZ) * scale,
                ),
                half = Vec3(cube.size.x * scale * 0.5, cube.size.y * scale * 0.5, cube.size.z * scale * 0.5),
                rotation = rotation,
            )
        }
        return CompiledLinkTemplate(boneName, cubes, width, thickness, length)
    }

    /** AABB of a bone subtree in the vehicle-local metre frame. */
    private fun localBounds(
        geometry: BedrockModelGeometry,
        bone: String,
        renderScale: Double,
    ): Array<Vec3> {
        val corners = geometry.subtreeCorners(bone)
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (corner in corners) {
            val local = geometry.toPhysicsLocal(corner, renderScale)
            minX = minOf(minX, local.x); maxX = maxOf(maxX, local.x)
            minY = minOf(minY, local.y); maxY = maxOf(maxY, local.y)
            minZ = minOf(minZ, local.z); maxZ = maxOf(maxZ, local.z)
        }
        return arrayOf(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
    }

    private fun failed(
        vehicleId: String,
        definition: TrackedVehicleDefinition,
        modelJson: String,
        findings: List<TrackedAuditFinding>,
    ) = TrackedVehicleSpec(
        vehicleId = vehicleId,
        model = definition.model,
        modelSha256 = sha256(modelJson),
        renderScale = definition.renderScale,
        massKg = definition.massKg,
        pitch = definition.track.pitch,
        pinInset = definition.track.pinInset,
        linkMassKg = definition.track.linkMass,
        angularLimitDegrees = definition.track.angularLimitDegrees,
        drivetrain = definition.drivetrain,
        sides = emptyMap(),
        audit = findings,
    )

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
