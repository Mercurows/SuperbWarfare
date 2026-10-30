package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.physics.tracked.legacy.LegacyShoeChainCompiler
import com.atsuishio.superbwarfare.physics.tracked.render.McspTrackPath
import cn.solarmoon.spark_core.physics.SuspensionRowSpec
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
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
 * The compiler is deliberately strict about *structure* (roles, bones, order,
 * ranges) and deliberately honest about *numbers*: it never silently rescales a
 * wheel, shifts a belt plane, or invents a missing member. Values that cannot be
 * satisfied are reported as errors; values that merely need a human decision are
 * warnings.
 *
 * An error never produces a usable specification: [TrackedCompilation.spec] is
 * null, so an invalid mechanism cannot reach the runtime through
 * [TrackedRunSpecs].
 *
 * ## Two paths, one direction
 *
 * The run topology is the mechanism: one dynamic box per adjacent belt member,
 * each attached to a wheel body at both ends. The 79-shoe chain is a **legacy
 * audit** that this compiler may also produce, and its failure is a warning.
 * Nothing on the run path reads the shoe chain, its solved link count, or the
 * idler offset it used to derive.
 */
private data class DraftWheel(
    val definition: TrackWheelDefinition,
    val index: Int,
    val bindCenter: Vec3,
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
            val template = compileRenderTemplate(
                side,
                definition.side(side),
                geometry,
                definition,
                sides[side],
                ::warn,
                ::info,
            )
            if (template != null) renders[side] = template
        }

        val totalStationMass = sides.values.flatMap { it.wheels }.filter { it.isSuspended }
            .sumOf { it.suspension.stationMassKg }
        if (totalStationMass >= definition.massKg) {
            error(
                "mass-budget",
                "sprung stations carry ${"%.1f".format(totalStationMass)} kg of a " +
                    "${"%.1f".format(definition.massKg)} kg vehicle; wheel bodies and runs also need mass",
            )
        }

        if (findings.any { it.severity == AuditSeverity.ERROR } || sides.size != TrackedSide.entries.size) {
            if (sides.size != TrackedSide.entries.size && findings.none { it.severity == AuditSeverity.ERROR }) {
                error("sides", "only ${sides.keys} compiled; both sides are required")
            }
            return TrackedCompilation(findings, null)
        }

        val runningGear = sides.values.sumOf { it.runningGearMassKg }
        val sprungAssembly = definition.massKg - runningGear
        val mass = TrackedMassAudit(
            sprungAssemblyMassKg = sprungAssembly,
            runningGearMassKg = runningGear,
            totalMassKg = definition.massKg,
            shoes = sides.values.mapNotNull { side ->
                side.legacy?.let { legacy ->
                    legacy.placements.firstOrNull()?.let { placement ->
                        TrackedMassProperties.box(
                            name = "${side.side.name.lowercase()}_shoe",
                            massKg = legacy.linkMassKg,
                            halfExtents = placement.halfExtents,
                        )
                    }
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
            runs = sides.values.flatMap { side ->
                side.runs.map { run ->
                    TrackedMassProperties.box(
                        name = "${side.side.name.lowercase()}_run_${run.index}",
                        massKg = run.massKg,
                        halfExtents = run.halfExtents,
                    )
                }
            },
        )
        if (!mass.closes()) {
            error(
                "mass-closure",
                "sprung assembly ${"%.3f".format(mass.sprungAssemblyMassKg)} + running gear " +
                    "${"%.3f".format(mass.runningGearMassKg)} kg does not equal " +
                    "${"%.3f".format(mass.totalMassKg)} kg",
            )
            return TrackedCompilation(findings, null)
        }
        if (!mass.detailCloses()) {
            error(
                "mass-detail",
                "running-gear detail does not add up: wheels " +
                    "${"%.3f".format(mass.wheels.sumOf { it.massKg })} + runs " +
                    "${"%.3f".format(mass.runs.sumOf { it.massKg })} kg",
            )
            return TrackedCompilation(findings, null)
        }
        if (!mass.sprungAssemblyMassKg.isFinite() || mass.sprungAssemblyMassKg <= 0.0) {
            error(
                "mass-budget",
                "sprung assembly ${"%.1f".format(mass.sprungAssemblyMassKg)} kg is not positive",
            )
            return TrackedCompilation(findings, null)
        }
        info(
            "mass",
            "sprung assembly ${"%.0f".format(mass.sprungAssemblyMassKg)} kg + running gear " +
                "${"%.0f".format(mass.runningGearMassKg)} kg = ${"%.0f".format(mass.totalMassKg)} kg",
        )

        val spec = TrackedVehicleSpec(
            vehicleId = vehicleId,
            model = definition.model,
            modelSha256 = sha256(modelJson),
            renderScale = definition.renderScale,
            chassisOriginX = definition.chassisOrigin.x,
            chassisOriginY = definition.chassisOrigin.y,
            chassisOriginZ = definition.chassisOrigin.z,
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
                accelMps2 = definition.drivetrain.accelMps2,
                brakeMps2 = definition.drivetrain.brakeMps2,
                pivotSpeedMps = definition.drivetrain.pivotSpeedMps,
                steerTimeSeconds = definition.drivetrain.steerTimeSeconds,
                tractionLimit = definition.drivetrain.tractionLimit,
            ),
            terrain = CompiledTerrain.compile(definition.terrain),
            sides = sides,
            audit = findings.toList(),
            renders = renders,
            mass = mass,
        )

        // The compiled result must be usable, not merely plausible. A spec the
        // adapter cannot express is an error here rather than a runtime surprise.
        val validation = TrackedCompiledSpecValidator.validate(spec)
        if (validation.any { it.severity == AuditSeverity.ERROR }) {
            findings += validation
            return TrackedCompilation(findings, null)
        }
        findings += validation
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

        val resolved = resolveWheels(sideDefinition, definition, geometry, label, error, warn) ?: return null
        if (resolved.isEmpty()) return null

        // Stable wheel table order: longitudinal, so an index does not depend on
        // the order someone happened to write the JSON list in. This ordering is
        // *not* the belt traversal; that comes from BeltOrder.
        val ordered = resolved
            .sortedWith(compareByDescending<DraftWheel> { it.bindCenter.z }.thenBy { it.definition.bone })
            .mapIndexed { index, wheel -> wheel.copy(index = index) }

        val beltOrder = resolveBeltOrder(sideDefinition, ordered, label, error) ?: return null
        val budget = runBudget(definition, sideDefinition, label, warn)

        val supportMass = ordered.filter { it.definition.role == TrackWheelRole.SUPPORT }
            .sumOf { it.definition.loadShare }
        val sprungPerSide = definition.massKg * 0.5 - budget - ordered.sumOf { it.definition.mass }
        if (sprungPerSide <= 0.0) {
            error(
                "$label.mass-budget",
                "$label running gear is more than half the vehicle mass; the springs would carry " +
                    "${"%.0f".format(sprungPerSide)} kg",
            )
        }

        val compiledWheels = ordered.mapIndexed { index, wheel ->
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
                index = index,
                bindCenter = wheel.bindCenter,
                assemblyCenter = wheel.bindCenter,
                radius = wheel.radius,
                halfWidth = wheel.width,
                axleDirection = wheel.definition.axleDirection.normalize(),
                massKg = wheel.definition.mass,
                suspension = suspension,
                tensioner = wheel.tensioner,
                wrap = wheel.definition.wrap ?: TrackWrap.OUTSIDE,
            )
        }

        val byIndex = compiledWheels.associateBy { it.index }
        val members = beltOrder.mapIndexed { ordinal, wheelIndex ->
            val wheel = byIndex[wheelIndex]
                ?: run {
                    error("$label.members", "$label belt order references missing wheel $wheelIndex")
                    return null
                }
            CompiledBeltMember(
                wheelIndex = wheelIndex,
                ordinal = ordinal,
                bone = wheel.bone,
                wrap = wheel.wrap,
            )
        }

        val belt = compileBeltGeometry(compiledWheels, sideDefinition, geometry, definition, label, error)
            ?: return null

        val runMasses = TrackedMassAllocator.allocateRuns(members, byIndex, budget)
        val total = TrackedMassAllocator.totalOf(runMasses)
        if (abs(total - budget) > 1.0e-6) {
            error(
                "$label.runs.mass",
                "$label run allocation totals ${"%.6f".format(total)} kg against the " +
                    "${"%.6f".format(budget)} kg budget",
            )
        }

        val runs = try {
            TrackedRunGeometry.compile(members, byIndex, belt, runMasses)
        } catch (failure: IllegalArgumentException) {
            error("$label.runs", "$label cannot build belt runs: ${failure.message}")
            return null
        }

        val runAudit = TrackedRunGeometryAudit.audit(runs, compiledWheels, belt)
        if (runAudit.interpenetrates) {
            error(
                "$label.run-interference",
                "$label run ${runAudit.worstRunIndex} overlaps wheel ${runAudit.worstWheelBone} by " +
                    "${"%.2f".format(-runAudit.minRunWheelGapM * 1000.0)} mm",
            )
        }
        if (runAudit.intersectingRunPairs.isNotEmpty()) {
            error(
                "$label.run-run",
                "$label non-adjacent runs intersect: ${runAudit.intersectingRunPairs}",
            )
        }

        val suspension = TrackedSuspensionCompiler.compile(compiledWheels, side)
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

        val mounts = compileMounts(compiledWheels, suspension, belt, label, error) ?: return null

        val legacy = LegacyShoeChainCompiler.compile(
            side = side,
            sideDefinition = sideDefinition,
            definition = definition,
            geometry = geometry,
            wheels = compiledWheels,
            warn = warn,
            info = info,
        )

        info(
            "$label.runs.summary",
            "$label runs: ${runs.size}, budget ${"%.0f".format(total)} kg, " +
                "length ${"%.3f".format(runs.minOf { it.lengthM })}.." +
                "${"%.3f".format(runs.maxOf { it.lengthM })} m, min run/wheel gap " +
                "${"%.2f".format(runAudit.minRunWheelGapM * 1000.0)} mm",
        )
        info(
            "$label.mass",
            "$label running gear ${"%.0f".format(runs.sumOf { it.massKg } + compiledWheels.sumOf { it.massKg })} kg, " +
                "sprung ${"%.0f".format(sprungPerSide)} kg per side",
        )

        return CompiledTrackSide(
            side = side,
            wheels = compiledWheels,
            belt = belt,
            beltOrder = beltOrder,
            members = members,
            runs = runs,
            mounts = mounts,
            suspension = suspension,
            legacy = legacy,
        )
    }

    /**
     * Resolve every authored wheel from its explicit bone.
     *
     * Nothing here infers a role, an index or an order from a bone name.
     */
    private fun resolveWheels(
        sideDefinition: TrackSideDefinition,
        definition: TrackedVehicleDefinition,
        geometry: BedrockModelGeometry,
        label: String,
        error: (String, String) -> Unit,
        warn: (String, String) -> Unit,
    ): List<DraftWheel>? {
        val resolved = ArrayList<DraftWheel>(sideDefinition.wheels.size)
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
            // this is a diagnostic rather than a hard limit.
            var meshExtent = 0.0
            for (corner in geometry.subtreeCorners(wheel.bone)) {
                val local = geometry.toPhysicsLocal(corner, definition.renderScale)
                meshExtent = max(meshExtent, hypot(local.y - rawPivot.y, local.z - rawPivot.z))
            }
            if (meshExtent > 0.0 && wheel.radius > meshExtent + 0.01) {
                warn(
                    "$label.wheel.radius",
                    "${wheel.bone} authored radius ${wheel.radius} m exceeds its mesh extent " +
                        "${"%.3f".format(meshExtent)} m",
                )
            }
            resolved += DraftWheel(
                definition = wheel,
                index = resolved.size,
                bindCenter = bind,
                radius = wheel.radius,
                width = wheel.halfWidth,
                tensioner = wheel.tensioner?.let {
                    CompiledTensioner(
                        travel = it.travel,
                        preloadN = it.preload,
                        dampingNsPerM = it.damping,
                        axis = it.axis.normalize(),
                    )
                },
            )
            if (wheel.role == TrackWheelRole.RETURN_ROLLER) {
                warn(
                    "$label.return-roller",
                    "${wheel.bone} is a fixed return roller and carries no ground load",
                )
            }
        }
        if (resolved.isEmpty()) return null
        return resolved
    }

    /**
     * Resolve the explicit belt traversal order to wheel indices.
     *
     * The order **is** the topology: it decides which runs exist. A missing order
     * is therefore an error, never a fallback to longitudinal sorting. Every
     * authored wheel must appear exactly once, so a typo cannot silently drop a
     * run or invent one.
     */
    private fun resolveBeltOrder(
        sideDefinition: TrackSideDefinition,
        wheels: List<DraftWheel>,
        label: String,
        error: (String, String) -> Unit,
    ): List<Int>? {
        if (sideDefinition.beltOrder.isEmpty()) {
            error(
                "$label.belt-order",
                "$label declares no BeltOrder. The run topology is that order; declare every belt member",
            )
            return null
        }
        val byBone = wheels.associateBy { it.definition.bone }
        val order = ArrayList<Int>(sideDefinition.beltOrder.size)
        for (bone in sideDefinition.beltOrder) {
            val wheel = byBone[bone]
            if (wheel == null) {
                error("$label.belt-order", "$label BeltOrder names unknown wheel $bone")
                return null
            }
            if (order.contains(wheel.index)) {
                error("$label.belt-order", "$label BeltOrder lists $bone twice")
                return null
            }
            order += wheel.index
        }
        val missing = wheels.filterNot { order.contains(it.index) }
        if (missing.isNotEmpty()) {
            error(
                "$label.belt-order",
                "$label BeltOrder omits ${missing.map { it.definition.bone }}; every belt member needs a run",
            )
            return null
        }
        if (order.size < 2) {
            error("$label.belt-order", "$label BeltOrder needs at least two wheels")
            return null
        }
        return order
    }

    /**
     * Resolve the belt's own geometry.
     *
     * The width and thickness come from the link template's authored cubes, and
     * the lateral centre from the template bone's own pivot. The wheel centres are
     * deliberately not averaged: on 99A that would shift the belt by more than
     * 160 mm.
     */
    private fun compileBeltGeometry(
        wheels: List<CompiledWheel>,
        sideDefinition: TrackSideDefinition,
        geometry: BedrockModelGeometry,
        definition: TrackedVehicleDefinition,
        label: String,
        error: (String, String) -> Unit,
    ): CompiledBeltGeometry? {
        val boneName = sideDefinition.linkTemplateBone
        if (boneName.isBlank() || boneName !in geometry.bones) {
            error("$label.template.missing", "missing link template bone $boneName")
            return null
        }
        val bone = geometry.bones[boneName] ?: return null
        if (bone.cubes.isEmpty()) {
            error("$label.template.cubes", "link template $boneName has no cubes")
            return null
        }
        val local = ArrayList<org.joml.Vector3d>()
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
        if (!(width > 0.0) || !(thickness > 0.0)) {
            error(
                "$label.template.size",
                "$label template $boneName has a degenerate cross-section: " +
                    "${"%.5f".format(width)} x ${"%.5f".format(thickness)} m",
            )
            return null
        }
        val centerX = geometry.toPhysicsLocal(geometry.worldPivot(boneName), definition.renderScale).x -
            definition.chassisOrigin.x

        // The belt plane must lie within every wheel's lateral span, or the run
        // and the wheel cannot be the same physical belt at all.
        for (wheel in wheels) {
            val wheelMin = wheel.assemblyCenter.x - wheel.halfWidth
            val wheelMax = wheel.assemblyCenter.x + wheel.halfWidth
            if (centerX + width * 0.5 < wheelMin || centerX - width * 0.5 > wheelMax) {
                error(
                    "$label.belt.plane",
                    "$label belt plane ${"%.4f".format(centerX)} m with width ${"%.4f".format(width)} m " +
                        "does not overlap ${wheel.bone} at ${"%.4f".format(wheel.assemblyCenter.x)} m",
                )
                return null
            }
        }

        return CompiledBeltGeometry(
            centerPlaneX = centerX,
            lateralAxis = Vec3(1.0, 0.0, 0.0),
            widthM = width,
            thicknessM = thickness,
            sizeSource = "bedrock cube bounds of $boneName, renderScale ${definition.renderScale}",
        )
    }

    /**
     * Resolve each wheel's mounting constraint from its authored role.
     *
     * Only a sprung road wheel gets a spring row; only an idler with a tensioner
     * gets a preload. A drive sprocket and a return roller are fixed, and so is an
     * idler without a tensioner. No mount leaves a free spin axis: the belt
     * carries the rolling contact and the visible wheel rotation is render-only.
     */
    private fun compileMounts(
        wheels: List<CompiledWheel>,
        suspension: SuspensionAudit,
        belt: CompiledBeltGeometry,
        label: String,
        error: (String, String) -> Unit,
    ): List<CompiledWheelMount>? {
        val rows = suspension.stations.associateBy { it.bone }
        val mounts = ArrayList<CompiledWheelMount>(wheels.size)
        for (wheel in wheels) {
            val wheelRef = CompiledBodyRef(CompiledBodyKind.WHEEL, wheel.side, wheel.index)
            val chassisRef = CompiledBodyRef(CompiledBodyKind.CHASSIS, wheel.side, -1)
            val name = "${wheel.side.name.lowercase()}_mount_${wheel.bone}"

            fun prismatic(axis: Vec3, up: Double, down: Double, springRow: SuspensionRowSpec? = null,
                          preload: Double? = null, damping: Double? = null): CompiledWheelMount {
                val frame = mountFrame(axis, belt)
                val plan = CompiledJointPlan(
                    name = name,
                    bodyA = chassisRef,
                    bodyB = wheelRef,
                    frameInA = CompiledConstraintFrame(wheel.bindCenter, frame),
                    frameInB = CompiledConstraintFrame(Vec3.ZERO, frame),
                    linear = listOf(
                        CompiledAxisLimit.Locked,
                        CompiledAxisLimit.Limited(-down, up),
                        CompiledAxisLimit.Locked,
                    ),
                    angular = List(3) { CompiledAxisLimit.Locked },
                )
                return CompiledWheelMount(
                    wheelIndex = wheel.index,
                    bone = wheel.bone,
                    kind = WheelMountKind.PRISMATIC,
                    axis = axis,
                    limitUpM = up,
                    limitDownM = down,
                    springRow = springRow,
                    preloadN = preload,
                    dampingNsPerM = damping,
                    joint = plan,
                )
            }

            fun fixed(): CompiledWheelMount = CompiledWheelMount(
                wheelIndex = wheel.index,
                bone = wheel.bone,
                kind = WheelMountKind.FIXED,
                axis = Vec3.ZERO,
                limitUpM = 0.0,
                limitDownM = 0.0,
                joint = CompiledJointPlan(
                    name = name,
                    bodyA = chassisRef,
                    bodyB = wheelRef,
                    frameInA = CompiledConstraintFrame(wheel.bindCenter, CompiledRotation.IDENTITY),
                    frameInB = CompiledConstraintFrame(Vec3.ZERO, CompiledRotation.IDENTITY),
                    linear = List(3) { CompiledAxisLimit.Locked },
                    angular = List(3) { CompiledAxisLimit.Locked },
                ),
            )

            val mount = when (wheel.role) {
                TrackWheelRole.SUPPORT -> {
                    val row = rows[wheel.bone]
                    if (row == null) {
                        error("$label.mount", "$label support wheel ${wheel.bone} has no suspension row")
                        return null
                    }
                    prismatic(
                        axis = wheel.suspension.direction,
                        up = wheel.suspension.travelUp,
                        down = wheel.suspension.travelDown,
                        springRow = row.toRowSpec(),
                    )
                }

                TrackWheelRole.IDLER_GUIDE -> {
                    val tensioner = wheel.tensioner
                    if (tensioner == null) {
                        // 99A's idler is fixed. A missing Tensioner is not a
                        // missing spring: the wheel stays on its bind pose, and
                        // belt length is not recovered by sliding it.
                        fixed()
                    } else {
                        prismatic(
                            axis = tensioner.axis,
                            up = tensioner.travel,
                            down = tensioner.travel,
                            preload = tensioner.preloadN,
                            damping = tensioner.dampingNsPerM,
                        )
                    }
                }

                TrackWheelRole.DRIVE_GUIDE, TrackWheelRole.RETURN_ROLLER -> fixed()
            }
            mounts += mount
        }
        return mounts
    }

    /**
     * Constraint frame for a mount: `X` is the wheel axle, `Y` is the travel axis,
     * `Z = X × Y`. Both axes are authored, so the frame is a real basis rather
     * than an assumption that travel is world `+Y`.
     */
    private fun mountFrame(travel: Vec3, belt: CompiledBeltGeometry): CompiledRotation {
        val y = travel.normalize()
        val x = belt.lateralAxis.normalize()
        require(abs(x.dot(y)) < 1.0e-6) { "mount travel $travel is not perpendicular to the belt axis ${belt.lateralAxis}" }
        val z = x.cross(y).normalize()
        return CompiledRotation.fromBasis(x, y, z)
    }

    /**
     * The belt budget for one side.
     *
     * An approved vehicle states it in its own resource. The fallback keeps an
     * older definition compiling, but it is reported: the run budget must not
     * silently follow a shoe count that the run topology does not have.
     */
    private fun runBudget(
        definition: TrackedVehicleDefinition,
        sideDefinition: TrackSideDefinition,
        label: String,
        warn: (String, String) -> Unit,
    ): Double {
        val authored = sideDefinition.runMassBudget
        if (authored > 0.0) return authored
        val fallback = definition.track.linkMass * definition.track.linkCount
        warn(
            "$label.run-budget",
            "$label declares no RunMassBudgetKg; using the legacy shoe budget " +
                "${"%.1f".format(fallback)} kg. Declare it before approval",
        )
        return fallback
    }

    private fun compileSuspension(
        definition: TrackSuspensionDefinition,
        stationMassKg: Double,
        label: String,
        bone: String,
        sprung: Boolean,
        error: (String, String) -> Unit,
    ): CompiledSuspension {
        // MCSP authors `Spring`/`Damper` directly. SBW keeps the
        // frequency/damping-ratio model as the default, but when an authored
        // direct rate is present it must be the value the force law actually
        // uses; otherwise the audit and the solver would disagree.
        val authoredStiffness = definition.springNPerM
        val stiffness = if (authoredStiffness != null) {
            authoredStiffness
        } else {
            val omega = 2.0 * PI * definition.frequency
            stationMassKg * omega * omega
        }
        val omega = if (stationMassKg > 0.0 && stiffness > 0.0) {
            sqrt(stiffness / stationMassKg)
        } else {
            0.0
        }
        val effectiveFrequency = omega / (2.0 * PI)
        val critical = 2.0 * sqrt(stiffness * stationMassKg)
        val authoredDamping = definition.damperNsPerM
        val compiled = CompiledSuspension(
            travelUp = definition.travelUp,
            travelDown = definition.travelDown,
            frequency = effectiveFrequency,
            stiffnessNPerM = stiffness,
            compressionDampingNsPerM = authoredDamping ?: (critical * definition.compressionDampingRatio),
            reboundDampingNsPerM = authoredDamping ?: (critical * definition.reboundDampingRatio),
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

    private fun compileRenderTemplate(
        side: TrackedSide,
        sideDefinition: TrackSideDefinition,
        geometry: BedrockModelGeometry,
        definition: TrackedVehicleDefinition,
        compiled: CompiledTrackSide?,
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
        val visiblePitch = templateLength(geometry, sideDefinition.linkTemplateBone, definition.renderScale)
        val envelope = compiled?.let { restEnvelope(it, geometry, definition, sideDefinition.linkTemplateBone, visiblePitch, warn) }
        info(
            "${side.name}.render",
            "${side.name}: ${bones.size} visible link slots under '$prefix'; visible pitch " +
                if (visiblePitch != null) "${"%.5f".format(visiblePitch)} m from the link template"
                else "unresolved",
        )
        return TrackedRenderTemplate(
            side = side,
            linkTemplateBone = sideDefinition.linkTemplateBone,
            visibleBones = bones,
            visiblePitchM = visiblePitch,
            thicknessM = compiled?.belt?.thicknessM ?: 0.0,
            visibleCount = envelope?.count ?: 0,
            restPitchM = envelope?.restPitch ?: 0.0,
            initialPhase = envelope?.phase ?: 0.0,
            seatAlongM = envelope?.seat?.along ?: 0.0,
            seatNormalM = envelope?.seat?.normal ?: 0.0,
            seatAngleRad = envelope?.seat?.angle ?: 0.0,
        )
    }

    private data class RestEnvelope(
        val count: Int,
        val restPitch: Double,
        val phase: Double,
        val seat: McspTrackPath.Seat,
    )

    /**
     * Count and seat are taken from the authored rest envelope, not the first
     * solved physics frame. A sagged first frame must not freeze the plate count.
     */
    private fun restEnvelope(
        compiled: CompiledTrackSide,
        geometry: BedrockModelGeometry,
        definition: TrackedVehicleDefinition,
        templateBone: String,
        visiblePitch: Double?,
        warn: (String, String) -> Unit,
    ): RestEnvelope? {
        if (visiblePitch == null || compiled.belt.thicknessM <= 0.0 || compiled.beltWheels.size < 2) return null
        val path = McspTrackPath.solve(
            compiled.beltWheels.map { wheel ->
                McspTrackPath.Wheel(
                    y = wheel.bindCenter.y,
                    z = wheel.bindCenter.z,
                    radius = wheel.radius,
                    outside = wheel.wrap == TrackWrap.OUTSIDE,
                )
            },
            compiled.belt.thicknessM,
        )
        if (path == null) {
            warn("${compiled.side.name}.render.envelope", "${compiled.side.name} visible belt does not close at rest")
            return null
        }
        val count = McspTrackPath.visibleCount(path.length, visiblePitch)
        if (count <= 0) return null
        val restPitch = path.length / count
        val pivot = geometry.toPhysicsLocal(geometry.worldPivot(templateBone), definition.renderScale)
        val origin = definition.chassisOrigin
        val forward = templateForward(geometry, templateBone)
        val seat = McspTrackPath.calibrate(
            path,
            pivot.y - origin.y,
            pivot.z - origin.z,
            forward.y,
            forward.z,
        )
        var phase = seat.distance / restPitch
        phase %= count.toDouble()
        if (phase < 0.0) phase += count.toDouble()
        return RestEnvelope(count, restPitch, phase, seat)
    }

    /** Model-space +Z of the template after its own authored rotation. */
    private fun templateForward(geometry: BedrockModelGeometry, boneName: String): Vector3d {
        val forward = Vector3d(0.0, 0.0, 1.0)
        val rotation = geometry.bones[boneName]?.rotation ?: return forward
        Matrix4d()
            .rotateZ(Math.toRadians(rotation.z))
            .rotateY(Math.toRadians(rotation.y))
            .rotateX(Math.toRadians(rotation.x))
            .transformDirection(forward)
        return forward
    }

    /**
     * Longitudinal extent of the single plate template, metres.
     *
     * This is the visible pitch candidate. It is not [TrackMechanismDefinition.pitch]
     * and it is not the belt length divided by the number of authored slots.
     */
    private fun templateLength(geometry: BedrockModelGeometry, boneName: String, renderScale: Double): Double? {
        val bone = geometry.bones[boneName] ?: return null
        if (bone.cubes.isEmpty()) return null
        var minZ = Double.POSITIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (corner in bone.cubes.flatMap { geometry.localCubeCorners(it) }) {
            minZ = minOf(minZ, corner.z)
            maxZ = maxOf(maxZ, corner.z)
        }
        val length = (maxZ - minZ) * renderScale / 16.0
        return length.takeIf { it.isFinite() && it > 1.0e-4 }
    }

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
