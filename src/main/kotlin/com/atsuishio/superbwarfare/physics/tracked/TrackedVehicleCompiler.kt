package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Turns a [TrackedVehicleDefinition] and its Bedrock asset into an immutable
 * [TrackedVehicleSpec], and reports every mechanism problem it can prove.
 *
 * The compiler is deliberately strict about *structure* (roles, bones, ranges)
 * and deliberately honest about *numbers*: it never silently rescales a wheel,
 * stretches a shoe or invents a missing pin. Values that cannot be satisfied are
 * reported as errors; values that merely need a human decision are warnings.
 *
 * An error never produces a usable specification: [TrackedCompilation.spec] is
 * null, so an invalid mechanism cannot reach the runtime through
 * [TrackedRunSpecs].
 */
private data class DraftWheel(
    val definition: TrackWheelDefinition,
    val index: Int,
    val bindCenter: Vec3,
    val assemblyCenter: Vec3,
    val radius: Double,
    val width: Double,
    val tensioner: CompiledTensioner?,
)

object TrackedVehicleCompiler {

    fun compile(
        vehicleId: String,
        definition: TrackedVehicleDefinition,
        modelJson: String,
    ): TrackedCompilation {
        val findings = ArrayList<TrackedAuditFinding>()
        fun error(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.ERROR, code, message))
        fun warn(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.WARN, code, message))
        fun info(code: String, message: String) =
            findings.add(TrackedAuditFinding(AuditSeverity.INFO, code, message))

        // Field validation runs before any geometry so a bad number is reported
        // as itself instead of surfacing later as a confusing solver failure.
        findings += TrackedDefinitionValidator.validate(definition)

        val geometry = try {
            BedrockModelGeometry.parse(modelJson)
        } catch (failure: Throwable) {
            error("model-parse", "cannot read geometry: ${failure.message}")
            return TrackedCompilation(findings, null)
        }

        val sides = LinkedHashMap<TrackedSide, CompiledTrackSide>()
        for (side in TrackedSide.entries) {
            val sideDefinition = definition.side(side)
            val compiled = compileSide(side, sideDefinition, definition, geometry, ::error, ::warn, ::info)
            if (compiled != null) sides[side] = compiled
        }

        val renders = LinkedHashMap<TrackedSide, TrackedRenderTemplate>()
        for (side in TrackedSide.entries) {
            val template = compileRenderTemplate(side, definition.side(side), geometry, ::warn, ::info)
            if (template != null) renders[side] = template
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

        if (findings.any { it.severity == AuditSeverity.ERROR } || sides.size != TrackedSide.entries.size) {
            if (sides.size != TrackedSide.entries.size && findings.none { it.severity == AuditSeverity.ERROR }) {
                error("sides", "only ${sides.keys} compiled; both sides are required")
            }
            return TrackedCompilation(findings, null)
        }

        val runningGear = sides.values.sumOf { it.runningGearMassKg }
        val hullMass = definition.massKg - runningGear
        val mass = TrackedMassAudit(
            hullMassKg = hullMass,
            runningGearMassKg = runningGear,
            totalMassKg = definition.massKg,
            shoes = sides.values.mapNotNull { side ->
                side.placements.firstOrNull()?.let { placement ->
                    TrackedMassProperties.box(
                        name = "${side.side.name.lowercase()}_shoe",
                        massKg = side.linkMassKg,
                        halfExtents = placement.halfExtents,
                    )
                }
            },
            wheels = sides.values.flatMap { side ->
                side.wheels.map { wheel ->
                    TrackedMassProperties.cylinder(
                        name = "${side.side.name.lowercase()}_${wheel.bone}",
                        massKg = wheel.massKg,
                        radius = wheel.radius,
                        halfWidth = wheel.halfWidth,
                        axleDirection = wheel.axleDirection,
                    )
                }
            },
        )
        if (!mass.hullMassKg.isFinite() || mass.hullMassKg <= 0.0) {
            error("mass-budget", "hull mass ${"%.1f".format(mass.hullMassKg)} kg is not positive")
            return TrackedCompilation(findings, null)
        }
        info(
            "mass",
            "hull ${"%.0f".format(mass.hullMassKg)} kg + running gear ${"%.0f".format(mass.runningGearMassKg)} kg " +
                "= ${"%.0f".format(mass.totalMassKg)} kg",
        )

        val spec = TrackedVehicleSpec(
            vehicleId = vehicleId,
            model = definition.model,
            modelSha256 = sha256(modelJson),
            renderScale = definition.renderScale,
            massKg = definition.massKg,
            pitch = definition.track.pitch,
            pinInset = definition.track.pinInset,
            clearanceM = definition.track.clearance,
            linkMassKg = definition.track.linkMass,
            angularLimitDegrees = definition.track.angularLimitDegrees,
            drivetrain = CompiledDrivetrain(
                maxDriveTorqueNm = definition.drivetrain.maxDriveTorque,
                maxBrakeTorqueNm = definition.drivetrain.maxBrakeTorque,
                maxSpeedMps = definition.drivetrain.maxSpeed,
                differential = definition.drivetrain.differential,
                rollingResistance = definition.drivetrain.rollingResistance,
                friction = definition.drivetrain.friction,
            ),
            sides = sides,
            audit = findings.toList(),
            renders = renders,
            mass = mass,
        )
        return TrackedCompilation(findings, spec)
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
        if (sideDefinition.wheels.isEmpty()) return null
        if (sideDefinition.linkTemplateBone.isBlank()) return null
        if (sideDefinition.linkTemplateBone !in geometry.bones) {
            error("$label.template.missing", "missing link template bone ${sideDefinition.linkTemplateBone}")
            return null
        }

        val linkTemplate = compileLinkTemplate(sideDefinition.linkTemplateBone, geometry, definition, label, error)
            ?: return null
        if (definition.track.pinInset > linkTemplate.thickness + 1.0e-6) {
            error(
                "$label.pin-inset",
                "PinInset ${definition.track.pinInset} m is outside the link thickness " +
                    "${"%.4f".format(linkTemplate.thickness)} m",
            )
        }

        // Resolve every wheel from its explicit bone. Ordering is by authored
        // longitudinal position so a side's stable indices do not depend on the
        // order someone happened to write the JSON list in.
        data class Resolved(
            val definition: TrackWheelDefinition,
            val bindCenter: Vec3,
            val radius: Double,
            val width: Double,
            val axle: Vec3,
        )

        val resolved = ArrayList<Resolved>(sideDefinition.wheels.size)
        for (wheel in sideDefinition.wheels) {
            if (wheel.bone.isBlank() || wheel.bone !in geometry.bones) {
                if (wheel.bone.isNotBlank()) error("$label.wheel.missing", "missing wheel bone ${wheel.bone}")
                continue
            }
            val rawPivot = geometry.toPhysicsLocal(geometry.worldPivot(wheel.bone), definition.renderScale)
            val origin = definition.chassisOrigin
            val bind = Vec3(rawPivot.x - origin.x, rawPivot.y - origin.y, rawPivot.z - origin.z)

            // Cross-check the authored radius against the mesh so a renamed or
            // re-scaled bone cannot silently leave a wheel the wrong size. The
            // mesh may legitimately be larger because it includes the arm, so
            // this is a diagnostic rather than a hard limit. Both points must be
            // in the same frame: the corners are vehicle-space, so the pivot is
            // too, before `ChassisOrigin` is subtracted.
            var meshExtent = 0.0
            for (corner in geometry.subtreeCorners(wheel.bone)) {
                val local = geometry.toPhysicsLocal(corner, definition.renderScale)
                meshExtent = maxOf(meshExtent, hypot(local.y - rawPivot.y, local.z - rawPivot.z))
            }
            if (meshExtent > 0.0 && wheel.radius > meshExtent + 0.01) {
                warn(
                    "$label.wheel.radius",
                    "${wheel.bone} authored radius ${wheel.radius} m exceeds its mesh extent " +
                        "${"%.3f".format(meshExtent)} m",
                )
            }
            resolved += Resolved(
                definition = wheel,
                bindCenter = bind,
                radius = wheel.radius,
                width = wheel.halfWidth,
                axle = wheel.axleDirection.normalize(),
            )
            if (wheel.role == TrackWheelRole.RETURN_ROLLER) {
                info("$label.return-roller", "${wheel.bone} is a fixed return roller and carries no ground load")
            }
        }

        if (resolved.isEmpty()) return null

        val ordered = resolved.sortedWith(
            compareByDescending<Resolved> { it.bindCenter.z }.thenBy { it.definition.bone },
        )
        val draftWheels = ordered.mapIndexed { index, wheel ->
            DraftWheel(
                definition = wheel.definition,
                index = index,
                bindCenter = wheel.bindCenter,
                assemblyCenter = wheel.bindCenter,
                radius = wheel.radius,
                width = wheel.width,
                tensioner = wheel.definition.tensioner?.let {
                    CompiledTensioner(
                        travel = it.travel,
                        preloadN = it.preload,
                        dampingNsPerM = it.damping,
                        axis = it.axis.normalize(),
                    )
                },
            )
        }

        val driveIndex = draftWheels.indexOfFirst { it.definition.role == TrackWheelRole.DRIVE_GUIDE }
        val idlerIndex = draftWheels.indexOfFirst { it.definition.role == TrackWheelRole.IDLER_GUIDE }
        if (driveIndex < 0 || idlerIndex < 0) return null

        val idlerTensioner = draftWheels[idlerIndex].tensioner
        if (idlerTensioner == null) {
            error("$label.tensioner", "$label idler has no tensioner block; the belt cannot be tensioned")
            return null
        }
        if (!isLongitudinal(idlerTensioner.axis)) {
            error(
                "$label.tensioner.axis",
                "$label tensioner axis ${idlerTensioner.axis} is not longitudinal; the S1 solver only moves the idler along Z",
            )
            return null
        }
        val tensionerTravel = idlerTensioner.travel

        val circles = draftWheels.map { wheel ->
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

        // The solver returns the pins of the *tensioned* loop. Publish the idler
        // assembly centre so no consumer has to re-apply the offset and risk
        // applying it twice (or not at all).
        val idlerOffset = Vec3(0.0, 0.0, loop.idlerOffset)
        val assemblyWheels = draftWheels.map { wheel ->
            if (wheel.index == idlerIndex) wheel.copy(assemblyCenter = wheel.bindCenter.add(idlerOffset)) else wheel
        }

        val linkMass = definition.track.linkMass
        val gearMass = linkMass * loop.linkCount + draftWheels.sumOf { it.definition.mass }
        val sprungPerSide = definition.massKg * 0.5 - gearMass
        if (sprungPerSide <= 0.0) {
            error(
                "$label.mass-budget",
                "$label running gear is ${"%.0f".format(gearMass)} kg against a " +
                    "${"%.0f".format(definition.massKg * 0.5)} kg half vehicle",
            )
        }
        val supportMass = draftWheels.filter { it.definition.role == TrackWheelRole.SUPPORT }
            .sumOf { it.definition.loadShare }
        val wheels = assemblyWheels.map { wheel ->
            val suspensionDefinition = wheel.definition.suspension?.applyTo(sideDefinition.suspension)
                ?: sideDefinition.suspension
            val share = if (wheel.definition.role == TrackWheelRole.SUPPORT && supportMass > 0.0) {
                sprungPerSide.coerceAtLeast(0.0) * wheel.definition.loadShare / supportMass
            } else {
                0.0
            }
            val suspension = compileSuspension(
                suspensionDefinition, share, label, wheel.definition.bone,
                sprung = wheel.definition.role == TrackWheelRole.SUPPORT, error = error,
            )
            CompiledWheel(
                side = side,
                role = wheel.definition.role,
                bone = wheel.definition.bone,
                index = wheel.index,
                bindCenter = wheel.bindCenter,
                assemblyCenter = wheel.assemblyCenter,
                radius = wheel.radius,
                halfWidth = wheel.width,
                axleDirection = wheel.definition.axleDirection.normalize(),
                massKg = wheel.definition.mass,
                suspension = suspension,
                tensioner = wheel.tensioner,
            )
        }
        info(
            "$label.mass",
            "$label running gear ${"%.0f".format(gearMass)} kg, sprung ${"%.0f".format(sprungPerSide)} kg per side",
        )

        val placements = TrackedAssemblyGeometry.place(
            pins = loop.pins,
            centerX = linkTemplate.centerX,
            boxCenterInPinFrame = linkTemplate.physical.boxCenterInPinFrame,
            halfExtents = linkTemplate.physical.halfExtents,
        )
        val geometryAudit = TrackedGeometryAudit.audit(
            placements = placements,
            wheels = wheels,
            shoeHalfWidth = linkTemplate.width * 0.5,
            pinChordClearanceM = loop.minPinChordClearanceM,
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
        if (geometryAudit.interpenetrates) {
            error(
                "$label.shoe-interference",
                "$label link ${geometryAudit.worstLinkIndex} overlaps wheel ${geometryAudit.worstWheelBone} by " +
                    "${"%.2f".format(-geometryAudit.minShoeWheelGapM * 1000.0)} mm at assembly; " +
                    "the pin locus must clear the shoe thickness (see ClosedTrackLoopSolver.pinLocusRadius)",
            )
        } else if (geometryAudit.minShoeWheelGapM < CompiledGeometryAudit.INTERPENETRATION_TOLERANCE_M) {
            warn(
                "$label.shoe-contact",
                "$label shoe touches wheel ${geometryAudit.worstWheelBone} at link ${geometryAudit.worstLinkIndex}: " +
                    "gap ${"%.2f".format(geometryAudit.minShoeWheelGapM * 1000.0)} mm",
            )
        }
        val travelUsed = abs(loop.idlerOffset) / tensionerTravel
        if (travelUsed > 0.9) {
            warn(
                "$label.tensioner.margin",
                "$label closes at ${"%.1f".format(loop.idlerOffset * 1000.0)} mm of a " +
                    "${"%.0f".format(tensionerTravel * 1000.0)} mm tensioner travel; only " +
                    "${"%.1f".format((1.0 - travelUsed) * 100.0)}% take-up remains",
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
                "length ${"%.4f".format(loop.totalLength)} m, idler offset " +
                "${"%.1f".format(loop.idlerOffset * 1000.0)} mm, max wrap " +
                "${"%.1f".format(loop.maxJointAngleDegrees)}°, " +
                "pin chord clearance ${"%.2f".format(loop.minPinChordClearanceM * 1000.0)} mm, " +
                "shoe gap ${"%.2f".format(geometryAudit.minShoeWheelGapM * 1000.0)} mm",
        )

        val suspension = TrackedSuspensionCompiler.compile(wheels, side)
        if (suspension.stationCount == 0) {
            error("$label.suspension", "$label has no sprung road wheel")
        }
        if (!suspension.staticSagFitsTravel) {
            error(
                "$label.suspension.sag",
                "$label static sag ${"%.4f".format(suspension.maxStaticSagM)} m exceeds compression travel; " +
                    "the wheel would rest on its bump stop",
            )
        } else if (!suspension.staticSagClearsBumpStop) {
            warn(
                "$label.suspension.sag",
                "$label static sag ${"%.4f".format(suspension.maxStaticSagM)} m sits on the bump stop",
            )
        }
        if (suspension.minForceMargin <= 1.0) {
            error(
                "$label.suspension.force",
                "$label row force cap cannot carry its station share (margin " +
                    "${"%.2f".format(suspension.minForceMargin)}x)",
            )
        }
        if (suspension.misalignedAxles.isNotEmpty()) {
            error("$label.suspension.axle", "$label non-lateral axles: ${suspension.misalignedAxles}")
        }
        if (suspension.cantedDirections.isNotEmpty()) {
            error("$label.suspension.direction", "$label non-vertical travel: ${suspension.cantedDirections}")
        }
        if (suspension.misalignedPivots.isNotEmpty()) {
            error(
                "$label.suspension.pivot",
                "$label constraint frames do not coincide at the bind pose: ${suspension.misalignedPivots}",
            )
        }
        if (!suspension.bilateralRecovery) {
            error(
                "$label.suspension.recovery",
                "$label suspension rows must produce a restoring force in droop",
            )
        }
        info(
            "$label.suspension.summary",
            "$label suspension: ${suspension.stationCount} rows, sprung " +
                "${"%.0f".format(suspension.sprungMassKg)} kg, max sag " +
                "${"%.4f".format(suspension.maxStaticSagM)} m, min force margin " +
                "${"%.2f".format(suspension.minForceMargin)}x",
        )

        return CompiledTrackSide(
            side = side,
            wheels = wheels,
            linkTemplate = linkTemplate,
            loop = loop,
            driveIndex = driveIndex,
            idlerIndex = idlerIndex,
            linkMassKg = linkMass,
            placements = placements,
            geometry = geometryAudit,
            suspension = suspension,
        )
    }

    private fun compileSuspension(
        definition: TrackSuspensionDefinition,
        stationMassKg: Double,
        label: String,
        bone: String,
        sprung: Boolean,
        error: (String, String) -> Unit,
    ): CompiledSuspension {
        val omega = 2.0 * PI * definition.frequency
        val stiffness = stationMassKg * omega * omega
        val critical = 2.0 * sqrt(stiffness * stationMassKg)
        val compiled = CompiledSuspension(
            travelUp = definition.travelUp,
            travelDown = definition.travelDown,
            frequency = definition.frequency,
            stiffnessNPerM = stiffness,
            compressionDampingNsPerM = critical * definition.compressionDampingRatio,
            reboundDampingNsPerM = critical * definition.reboundDampingRatio,
            maxForceN = definition.maxForce,
            bumpStopStart = definition.bumpStopStart,
            bumpStopExponent = definition.bumpStopExponent,
            direction = definition.direction.normalize(),
            stationMassKg = stationMassKg,
            staticCompressionM = if (omega > 1.0e-6) 9.81 / (omega * omega) else 0.0,
        )
        if (!compiled.stiffnessNPerM.isFinite() || !compiled.compressionDampingNsPerM.isFinite() ||
            !compiled.reboundDampingNsPerM.isFinite() || !compiled.staticCompressionM.isFinite()
        ) {
            error("$label.suspension.derived", "$bone derived spring/damper values are not finite")
        }
        if (sprung && stationMassKg > 0.0 && definition.maxForce < stationMassKg * 9.81) {
            error(
                "$label.max-force",
                "$bone MaxForce ${definition.maxForce} N cannot carry its " +
                    "${"%.0f".format(stationMassKg)} kg share",
            )
        }
        if (sprung && stationMassKg <= 0.0) {
            error("$label.station-mass", "$bone is a support wheel but received no sprung mass")
        }
        return compiled
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
            error("$label.template.cubes", "$label template $boneName produced no corners")
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
        if (!(width > 0.0) || !(thickness > 0.0) || !(length > 0.0)) {
            error(
                "$label.template.size",
                "$label template $boneName has a degenerate extent: ${"%.5f".format(width)} x " +
                    "${"%.5f".format(thickness)} x ${"%.5f".format(length)} m",
            )
            return null
        }
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

        // The physical shoe is the AABB of the template in the pin frame. For a
        // single unrotated cube this is exact; for anything richer it is a
        // conservative box, which is the only shape the core `TrackRunSpec`
        // currently accepts, and it is reported rather than silently assumed.
        val boxCenter = Vec3(
            ((minX + maxX) * 0.5 - (minX + maxX) * 0.5) * scale,
            ((minY + maxY) * 0.5 + shiftY) * scale,
            ((minZ + maxZ) * 0.5 + shiftZ) * scale,
        )
        val simple = bone.cubes.size == 1 &&
            bone.cubes.first().rotation == null &&
            bone.cubes.first().inflate == 0.0
        if (!simple) {
            error(
                "$label.template.shape",
                "$label template $boneName has ${bone.cubes.size} cube(s) with rotation or inflate; " +
                    "the physical shoe would be an AABB approximation. Author a single plain cube",
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

    private fun compileRenderTemplate(
        side: TrackedSide,
        sideDefinition: TrackSideDefinition,
        geometry: BedrockModelGeometry,
        warn: (String, String) -> Unit,
        info: (String, String) -> Unit,
    ): TrackedRenderTemplate? {
        val prefix = sideDefinition.visibleBonePrefix
        if (prefix.isNullOrBlank()) {
            info("${side.name}.render", "${side.name} declares no VisibleBonePrefix; no visible description produced")
            return null
        }
        val bones = geometry.bones.keys.filter { it.startsWith(prefix) }.sorted()
        if (bones.isEmpty()) {
            warn("${side.name}.render.missing", "${side.name} visible prefix '$prefix' matches no bone")
            return null
        }
        info(
            "${side.name}.render",
            "${side.name}: ${bones.size} visible link bones under '$prefix'; visible pitch unresolved " +
                "(the model carries no spacing)",
        )
        return TrackedRenderTemplate(
            side = side,
            linkTemplateBone = sideDefinition.linkTemplateBone,
            visibleBones = bones,
            visiblePitchM = null,
        )
    }

    private fun isLongitudinal(axis: Vec3): Boolean =
        abs(abs(axis.z) - 1.0) < 1.0e-6 && abs(axis.x) < 1.0e-6 && abs(axis.y) < 1.0e-6

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
