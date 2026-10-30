package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.physics.tracked.render.BeltContactBaseline
import com.atsuishio.superbwarfare.physics.tracked.render.McspTrackPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * S1 audit of the wheel-pair run topology.
 *
 * This is a pure data test: it never creates a Bullet object, never reads a
 * Level and never touches the renderer. It answers five separate questions:
 *
 * 1. Does the authored belt order produce exactly one run per adjacent pair,
 *    including the last pair back to the first?
 * 2. Is the run box on the belt's own plane, outside the wheel's collider, with
 *    the belt's thickness applied on the correct side?
 * 3. Are both attachment anchors the wheel centres, and is each end a complete
 *    six-axis plan rather than a label?
 * 4. Does the mass split conserve the authored budget exactly?
 * 5. Do two-wheel belts, all four wrap combinations and degenerate inputs behave
 *    correctly?
 *
 * Nothing here proves the mechanism is dynamically stable. That is B2.
 */
class TrackedRunGeometryTest {
    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun definition(): TrackedVehicleDefinition = json.decodeFromString(
        TrackedVehicleDefinition.serializer(),
        File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
    )

    private fun spec(): TrackedVehicleSpec = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        definition(),
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
    ).requireSpec()

    @Test
    fun theBeltOrderIsExplicitAndComplete() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(8, compiled.beltOrder.size, "$side belt order length")
            assertEquals(8, compiled.beltOrder.distinct().size, "$side belt order has no repeats")
            assertEquals(
                compiled.wheels.map { it.index }.toSet(),
                compiled.beltOrder.toSet(),
                "$side belt order covers every wheel",
            )
            assertEquals(
                compiled.beltOrder.indices.toList(),
                compiled.members.map { it.ordinal },
                "$side member ordinals must be 0..n in order",
            )
            compiled.members.forEachIndexed { ordinal, member ->
                assertEquals(compiled.beltOrder[ordinal], member.wheelIndex, "$side member $ordinal wheel")
                assertEquals(compiled.wheels[member.wheelIndex].wrap, member.wrap, "$side member $ordinal wrap")
            }
        }
    }

    @Test
    fun oneRunExistsPerAdjacentPairIncludingTheClosingPair() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(compiled.beltOrder.size, compiled.runs.size, "$side run count")
            compiled.runs.forEachIndexed { index, run ->
                assertEquals(index, run.index, "$side run index")
                assertEquals(compiled.beltOrder[index], run.firstWheelIndex, "$side run $index first wheel")
                assertEquals(
                    compiled.beltOrder[(index + 1) % compiled.beltOrder.size],
                    run.secondWheelIndex,
                    "$side run $index second wheel",
                )
            }
            assertTrue(compiled.runs.last().closesLoop, "$side last run must close the loop")
            assertEquals(1, compiled.runs.count { it.closesLoop }, "$side must have exactly one closing run")
        }
    }

    /**
     * The box is the belt, not the wheel: its lateral half extent is the authored
     * belt width and its normal half extent is half the belt thickness.
     */
    @Test
    fun everyRunUsesTheBeltWidthAndDepth() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val belt = compiled.belt
            for (run in compiled.runs) {
                assertEquals(belt.widthM * 0.5, run.halfExtents.x, 1e-9, "$side run ${run.index} half width")
                assertEquals(belt.halfThicknessM, run.halfExtents.y, 1e-9, "$side run ${run.index} half depth")
                assertTrue(
                    run.halfExtents.x > compiled.wheels.first().halfWidth,
                    "$side run ${run.index} is not wider than the wheel",
                )
            }
        }
    }

    /**
     * The run must sit **outside** the wheel's collider, not inside it.
     *
     * The previous version of this test checked only that the far face was one
     * radius away from the wheel centre, which is also true of a box that
     * straddles the wheel by a full thickness. This checks the near face too.
     */
    @Test
    fun everyRunClearsItsWheelsInsteadOfStraddlingThem() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val audit = TrackedRunGeometryAudit.audit(compiled.runs, compiled.wheels, compiled.belt)
            assertEquals(
                emptyList<String>(),
                audit.problems(),
                "$side run/wheel audit: ${audit.problems()}",
            )
            assertTrue(audit.checkedPairs > 0, "$side audit checked no run/wheel pair")
            for (run in compiled.runs) {
                val first = compiled.wheels[run.firstWheelIndex]
                val second = compiled.wheels[run.secondWheelIndex]
                for (wheel in listOf(first, second)) {
                    val local = TrackedRunGeometry.inverseRotateAboutX(
                        wheel.assemblyCenter.subtract(run.center),
                        run.rotation,
                    )
                    // Near face distance from the wheel centre must be at least the
                    // wheel radius: the belt sits outside the wheel's solid body.
                    val nearFace = abs(local.y) - run.halfExtents.y
                    assertTrue(
                        nearFace >= wheel.radius - 2.0e-6,
                        "$side run ${run.index} near face $nearFace is inside ${wheel.bone} radius ${wheel.radius}",
                    )
                }
            }
        }
    }

    /**
     * Bottom road-wheel runs must extend below the metal wheel.
     *
     * The hull boxes sit about half a metre above the road-wheel bottoms. If a
     * run's outer face is not below those bottoms, closing wheel-terrain contact
     * is the only thing keeping the visual hull out of the ground.
     */
    @Test
    fun roadWheelRunsReachBelowTheMetalWheel() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val roads = compiled.runs.filter { run ->
                compiled.wheels[run.firstWheelIndex].role == TrackWheelRole.SUPPORT &&
                    compiled.wheels[run.secondWheelIndex].role == TrackWheelRole.SUPPORT
            }
            assertEquals(5, roads.size, "$side bottom road-wheel runs")
            for (run in roads) {
                for (wheel in listOf(compiled.wheels[run.firstWheelIndex], compiled.wheels[run.secondWheelIndex])) {
                    val outerY = outerFaceY(run, wheel)
                    val wheelBottom = wheel.assemblyCenter.y - wheel.radius
                    val drop = wheelBottom - outerY
                    assertTrue(
                        drop > compiled.belt.halfThicknessM * 0.5,
                        "$side run ${run.index} outer face is only $drop m below ${wheel.bone}; " +
                            "wheelBottom=$wheelBottom outerY=$outerY",
                    )
                    assertTrue(
                        drop < compiled.belt.thicknessM * 2.0,
                        "$side run ${run.index} outer face is $drop m below ${wheel.bone}, not one belt thickness",
                    )
                }
            }
        }
    }

    /**
     * Local +Y is the belt outward normal. A flipped run still has one face
     * outside the wheel, so the face distance alone cannot catch it.
     */
    @Test
    fun everyRunNormalPointsOutOfItsWheels() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            for (run in compiled.runs) {
                val outward = TrackedAssemblyGeometry.rotateAboutX(Vec3(0.0, 1.0, 0.0), angleOf(run.rotation))
                for (index in listOf(run.firstWheelIndex, run.secondWheelIndex)) {
                    val wheel = compiled.wheels[index]
                    val fromWheel = run.center.subtract(wheel.assemblyCenter)
                    val alignment = outward.x * fromWheel.x + outward.y * fromWheel.y + outward.z * fromWheel.z
                    assertTrue(
                        alignment > wheel.radius * 0.25,
                        "$side run ${run.index} local +Y points into ${wheel.bone}: alignment $alignment",
                    )
                }
            }
        }
    }

    /**
     * The belt's lowest outer face must be below the solid hull boxes.
     *
     * Those boxes are what the hull rests on if the runs and wheels miss the
     * terrain. Their bottoms are about half a metre above the road wheels.
     */
    @Test
    fun theLowestRoadRunIsBelowTheSolidHull() {
        val spec = spec()
        val hullBottom = solidHullBottomLocal(spec)
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val lowest = compiled.runs.filter { run ->
                compiled.wheels[run.firstWheelIndex].role == TrackWheelRole.SUPPORT &&
                    compiled.wheels[run.secondWheelIndex].role == TrackWheelRole.SUPPORT
            }.minOf { run ->
                minOf(
                    outerFaceY(run, compiled.wheels[run.firstWheelIndex]),
                    outerFaceY(run, compiled.wheels[run.secondWheelIndex]),
                )
            }
            assertTrue(
                lowest < hullBottom - 0.2,
                "$side belt outer $lowest is not below the solid hull bottom $hullBottom",
            )
        }
    }

    /**
     * A run that is deliberately moved into its wheel must be rejected.
     *
     * This is the negative case the old "outer face is tangent" assertion could
     * not catch: the outer face still looks correct while the box overlaps the
     * wheel by a full thickness.
     */
    @Test
    fun aRunInsideItsWheelIsRejectedByTheAudit() {
        val spec = spec()
        val compiled = spec.side(TrackedSide.LEFT)
        val belt = compiled.belt
        val run = compiled.runs.first()
        val wheel = compiled.wheels[run.firstWheelIndex]
        // Move the box inward along its own normal, so it genuinely passes
        // through the wheel instead of sliding sideways.
        val local = TrackedRunGeometry.inverseRotateAboutX(
            wheel.assemblyCenter.subtract(run.center),
            run.rotation,
        )
        // Local +Y points from the wheel centre towards the belt, so moving along
        // -Y is towards the wheel.
        val inwardLocal = Vec3(0.0, -belt.thicknessM, 0.0)
        val inwardWorld = TrackedAssemblyGeometry.rotateAboutX(inwardLocal, angleOf(run.rotation))
        val moved = run.copy(center = run.center.add(inwardWorld))
        val audit = TrackedRunGeometryAudit.audit(listOf(moved), listOf(wheel), belt)
        assertTrue(
            audit.minRunWheelGapM < -TrackedRunGeometryAudit.CONTACT_TOLERANCE_M,
            "an inward run must overlap the wheel, gap ${audit.minRunWheelGapM}",
        )
        assertTrue(audit.interpenetrates)
    }

    @Test
    fun runLengthSpansTheBeltCentreLinePlusItsOwnThickness() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val belt = compiled.belt
            for (run in compiled.runs) {
                val first = compiled.wheels[run.firstWheelIndex]
                val second = compiled.wheels[run.secondWheelIndex]
                val offset = belt.runCenterOffset(first.radius)
                val offsetSecond = belt.runCenterOffset(second.radius)
                val chord = hypot(
                    second.assemblyCenter.y - first.assemblyCenter.y,
                    second.assemblyCenter.z - first.assemblyCenter.z,
                )
                // The contact points are perpendicular to the tangent normal, so
                // their separation is the Pythagorean leg, not the hypotenuse.
                val delta = offset - offsetSecond
                val expected = sqrt(chord * chord - delta * delta) + belt.thicknessM
                assertEquals(expected, run.lengthM, 1e-6, "$side run ${run.index} length")
            }
        }
    }

    /**
     * Both anchors are the wheel centres, and each end carries a complete plan.
     */
    @Test
    fun bothAttachmentAnchorsAreTheWheelCentres() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            for (run in compiled.runs) {
                for ((label, attachment) in listOf("first" to run.first, "second" to run.second)) {
                    val wheel = compiled.wheels[attachment.wheelIndex]
                    assertEquals(0.0, attachment.anchorInWheel.length(), 1e-12, "$label anchor in wheel")
                    val world = run.center.add(
                        TrackedAssemblyGeometry.rotateAboutX(attachment.anchorInRun, angleOf(run.rotation)),
                    )
                    assertEquals(
                        0.0,
                        world.distanceTo(wheel.assemblyCenter),
                        2.0e-6,
                        "$side run ${run.index} $label anchor must land on ${wheel.bone}",
                    )
                }
                assertTrue(run.first.isFirst, "first attachment must be the revolute end")
                assertFalse(run.second.isFirst, "second attachment must be the normal end")
            }
        }
    }

    /**
     * The first end is a real revolute: all translation locked, only the axle
     * rotation free. The second end holds exactly one translation and frees
     * everything else, with a non-zero normal.
     */
    @Test
    fun bothRunEndsCarryCompleteSixAxisPlans() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            for (run in spec.side(side).runs) {
                val first = run.first.joint
                assertTrue(first.isFirstRunEnd, "${run.index} first end is not a revolute: ${first.linear}")
                assertEquals(
                    listOf(0),
                    first.freeAngularAxes,
                    "${run.index} first end must free only the axle rotation",
                )
                assertEquals(CompiledBodyKind.WHEEL, first.bodyA.kind)
                assertEquals(CompiledBodyKind.RUN, first.bodyB.kind)

                val second = run.second.joint
                assertTrue(second.isSecondRunEnd, "${run.index} second end is not a single-normal hold")
                assertEquals(CompiledBodyKind.RUN, second.bodyA.kind)
                assertEquals(CompiledBodyKind.WHEEL, second.bodyB.kind)
                assertEquals(3, second.freeAngularAxes.size, "second end must free every rotation")

                val normal = run.second.lockedNormalLocal
                assertTrue(normal != null, "${run.index} second end declares no normal")
                assertEquals(1.0, normal!!.length(), 1e-6, "${run.index} normal must be a unit vector")
                assertTrue(normal.length() > 0.0, "${run.index} normal must not be zero")

                // The locked axis of the second end must be the run's normal, not
                // an arbitrary world direction: rotate the joint frame back.
                val frameRotation = second.frameInA.rotation
                val lockedAxis = TrackedAssemblyGeometry.rotateAboutX(
                    Vec3(0.0, 1.0, 0.0),
                    angleOf(frameRotation),
                )
                assertEquals(
                    1.0,
                    abs(lockedAxis.dot(normal)),
                    1e-6,
                    "${run.index} locked joint axis $lockedAxis must be the run normal $normal",
                )
            }
        }
    }

    @Test
    fun everyWheelKeepsItsAuthoredMassAndNoSpinAxis() {
        val spec = spec()
        val definition = definition()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val authored = definition.side(side).wheels.associateBy { it.bone }
            for (wheel in compiled.wheels) {
                assertEquals(authored.getValue(wheel.bone).mass, wheel.massKg, 1e-9, "${wheel.bone} mass")
                val mount = compiled.mount(wheel.index)
                assertTrue(mount.locksAllRotation, "${wheel.bone} mount leaves a rotation free")
            }
        }
    }

    /**
     * Option A: the run budget is authored per side, split by combined wheel
     * mass. Conservation is exact in `Double`, so the vehicle total does not
     * move; the `Float` adapter total may differ by a few micrograms.
     */
    @Test
    fun theRunMassSplitConservesTheAuthoredBudget() {
        val spec = spec()
        val definition = definition()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val budget = definition.side(side).runMassBudget
            assertTrue(budget > 0.0, "$side must author RunMassBudgetKg")
            val total = compiled.runMassTotalKg
            assertEquals(budget, total, 1e-6, "$side run mass must equal the authored budget")
            val wheels = compiled.beltWheels
            val pairMassTotal = wheels.indices.sumOf { i ->
                wheels[i].massKg + wheels[(i + 1) % wheels.size].massKg
            }
            compiled.runs.forEachIndexed { index, run ->
                val pair = wheels[index].massKg + wheels[(index + 1) % wheels.size].massKg
                assertEquals(
                    budget * pair / pairMassTotal,
                    run.massKg,
                    1e-6,
                    "$side run $index mass share",
                )
                assertTrue(run.massKg > 0.0, "$side run $index needs positive mass")
            }
        }
    }

    @Test
    fun roadWheelBaselinesSitOnTheBeltCentreLine() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
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
            assertNotNull(path, "$side visible envelope must close with a fixed idler")
            val solved = path!!
            val roads = compiled.beltWheels.filter { it.role == TrackWheelRole.SUPPORT }
            assertEquals(6, roads.size, "$side road wheels")
            for (wheel in roads) {
                val plate = BeltContactBaseline.nearest(solved, wheel.bindCenter.y, wheel.bindCenter.z)
                assertNotNull(plate, "${wheel.bone} has no centre-line sample")
                val actual = hypot(plate!!.y - wheel.bindCenter.y, plate.z - wheel.bindCenter.z)
                val centre = wheel.radius + compiled.belt.halfThicknessM
                assertEquals(centre, actual, 0.02, "${wheel.bone} centre-line radius")
                assertTrue(plate.y < wheel.bindCenter.y, "${wheel.bone} baseline must be below the hub")
            }
            val rest = spec.renders[side]?.let { it.restPitchM * it.visibleCount }
            assertNotNull(rest, "$side rest length")
            assertEquals(solved.length, rest!!, 1e-6, "$side rest length is the fixed envelope")
        }
    }

    @Test
    fun theVehicleMassStillCloses() {
        val spec = spec()
        assertEquals(55_000.0, spec.mass.totalMassKg, 1e-6)
        assertTrue(spec.mass.closes(), "sprung assembly + running gear must equal the authored mass")
        assertEquals(
            spec.mass.totalMassKg,
            spec.mass.sprungAssemblyMassKg + spec.mass.runningGearMassKg,
            1e-6,
        )
        assertTrue(spec.mass.detailCloses(), "wheel and run detail must add up to running gear")
    }

    @Test
    fun everyBeltMemberHasExactlyOneMount() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(compiled.wheels.size, compiled.mounts.size, "$side mount count")
            assertEquals(
                compiled.wheels.size,
                compiled.mounts.map { it.wheelIndex }.distinct().size,
                "$side mounts must be one per wheel",
            )
            for (wheel in compiled.wheels) {
                val mount = compiled.mount(wheel.index)
                assertEquals(wheel.bone, mount.bone)
                when (wheel.role) {
                    TrackWheelRole.SUPPORT -> {
                        assertEquals(WheelMountKind.PRISMATIC, mount.kind, "${wheel.bone} mount kind")
                        assertTrue(mount.isSprung, "${wheel.bone} must carry a spring row")
                        assertEquals(wheel.suspension.travelUp, mount.limitUpM, 1e-9)
                        assertEquals(wheel.suspension.travelDown, mount.limitDownM, 1e-9)
                        assertEquals(1.0, abs(mount.axis.y), 1e-9, "${wheel.bone} travel must be vertical")
                        assertFalse(mount.isTensioned, "${wheel.bone} must not carry a tensioner")
                        val plan = mount.joint!!
                        assertEquals(listOf(1), plan.freeLinearAxes, "${wheel.bone} must move on Y only")
                    }

                    TrackWheelRole.IDLER_GUIDE -> {
                        assertEquals(WheelMountKind.FIXED, mount.kind, "${wheel.bone} mount kind")
                        assertFalse(mount.isTensioned, "${wheel.bone} must stay fixed")
                        assertFalse(mount.isSprung, "${wheel.bone} must not carry a spring row")
                        assertTrue(mount.joint!!.freeLinearAxes.isEmpty(), "${wheel.bone} must not translate")
                        assertTrue(mount.joint!!.freeAngularAxes.isEmpty(), "${wheel.bone} must not rotate")
                    }

                    TrackWheelRole.DRIVE_GUIDE, TrackWheelRole.RETURN_ROLLER -> {
                        assertEquals(WheelMountKind.FIXED, mount.kind, "${wheel.bone} mount kind")
                        assertFalse(mount.isSprung, "${wheel.bone} must not carry a spring row")
                        assertFalse(mount.isTensioned, "${wheel.bone} must not carry a tensioner")
                        assertTrue(mount.joint!!.freeLinearAxes.isEmpty(), "${wheel.bone} must be fully fixed")
                    }
                }
            }
        }
    }

    @Test
    fun eachSideProducesOneSingleLinkRunSpecPerRun() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val runs = TrackedRunSpecs.compileRuns(spec, side)
            assertEquals(compiled.runs.size, runs.size, "$side run spec count")
            runs.forEachIndexed { index, runSpec ->
                assertTrue(runSpec.problems().isEmpty(), "$side run $index problems: ${runSpec.problems()}")
                assertEquals(1, runSpec.links.size, "$side run $index must be a single link")
                assertTrue(runSpec.joints.isEmpty(), "$side run $index must have no internal joint")
                assertEquals(compiled.runs[index].massKg.toFloat(), runSpec.links.first().massKg)
                assertEquals(compiled.runs[index].center, runSpec.links.first().position)
            }
            assertEquals(
                compiled.runMassTotalKg,
                TrackedRunSpecs.runMassTotalKg(spec, side),
                1e-4,
                "$side run specs must carry the audited budget",
            )
        }
    }

    @Test
    fun runNamesAreStableAndVehicleScoped() {
        val spec = spec()
        val names = TrackedSide.entries.flatMap { side ->
            TrackedRunSpecs.compileRuns(spec, side).map { it.name }
        }
        assertEquals(names.size, names.distinct().size, "run names must be unique")
        assertTrue(names.all { it.startsWith("superbwarfare:ztz_99a_track_") }, "run names: $names")
        assertTrue(names.all { "_run_" in it }, "run names must identify the pair: $names")
    }

    @Test
    fun theCompiledSpecPassesItsOwnFinalValidation() {
        val spec = spec()
        assertEquals(emptyList<TrackedAuditFinding>(), TrackedCompiledSpecValidator.validate(spec))
        assertTrue(TrackedCompiledSpecValidator.isClean(spec))
        for (side in TrackedSide.entries) {
            assertEquals(
                emptyList<String>(),
                TrackedCompiledSpecValidator.runSpecProblems(spec, side),
                "$side adapter problems",
            )
        }
    }

    /**
     * Lowest solid hull box in the chassis frame.
     *
     * Matches the production filter: the legacy Collision envelope is skipped
     * when detailed hull parts exist, and wheel/turret/barrel parts are not hull.
     */
    private fun solidHullBottomLocal(spec: TrackedVehicleSpec): Double {
        val root = json.parseToJsonElement(
            File(resources, "data/superbwarfare/sbw/vehicles/ztz_99a.json").readText(),
        ).jsonObject
        val parts = root.getValue("OBB").jsonArray.map { it.jsonObject }
        fun text(part: JsonObject, key: String): String? = part[key]?.jsonPrimitive?.content
        val detailed = parts.any { part ->
            val name = text(part, "Part")
            val transform = text(part, "Transform")
            !name.equals("Collision", true) &&
                !name.equals("WheelLeft", true) &&
                !name.equals("WheelRight", true) &&
                (transform == null || transform.equals("default", true))
        }
        val bottoms = parts.mapNotNull { part ->
            val name = text(part, "Part")
            val transform = text(part, "Transform")?.lowercase()
            if (detailed && name.equals("Collision", true)) return@mapNotNull null
            if (name.equals("WheelLeft", true) || name.equals("WheelRight", true)) return@mapNotNull null
            if (transform == "turret" || transform == "barrel" || transform?.contains("weapon") == true) {
                return@mapNotNull null
            }
            val position = part.getValue("Position").jsonArray
            val size = part.getValue("Size").jsonArray
            position[1].jsonPrimitive.double - size[1].jsonPrimitive.double
        }
        assertTrue(bottoms.isNotEmpty(), "99A has no solid hull box")
        return bottoms.min() - spec.chassisOriginY
    }

    /** World Y of the run face opposite the wheel, in the chassis frame. */
    private fun outerFaceY(run: CompiledRun, wheel: CompiledWheel): Double {
        val local = TrackedRunGeometry.inverseRotateAboutX(
            wheel.assemblyCenter.subtract(run.center),
            run.rotation,
        )
        val outwardSign = if (local.y >= 0.0) -1.0 else 1.0
        val outward = TrackedAssemblyGeometry.rotateAboutX(
            Vec3(0.0, outwardSign * run.halfExtents.y, 0.0),
            angleOf(run.rotation),
        )
        return run.center.y + outward.y
    }

    private fun angleOf(rotation: CompiledRotation): Float =
        2f * kotlin.math.atan2(rotation.x, rotation.w)
}
