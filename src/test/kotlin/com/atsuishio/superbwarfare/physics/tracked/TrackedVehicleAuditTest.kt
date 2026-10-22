package com.atsuishio.superbwarfare.physics.tracked

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * S1 data/geometry audit of the first real tracked vehicle object.
 *
 * This is deliberately a plain JVM test: the mechanism contract must be
 * verifiable without a Minecraft runtime, so a geometry or mass regression can
 * never hide behind a game test that takes minutes to boot.
 *
 * Nothing here proves the vehicle drives. It only proves that the authored
 * mechanism data, the Bedrock geometry and the closed-loop assembly agree, and
 * it records the physical/visible/skeleton counts separately.
 */
class TrackedVehicleAuditTest {
    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val definitionFile = File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json")
    private val vehicleFile = File(resources, "data/superbwarfare/sbw/vehicles/ztz_99a.json")
    private val modelFile = File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json")

    private fun definition(): TrackedVehicleDefinition = json.decodeFromString(
        TrackedVehicleDefinition.serializer(),
        definitionFile.readText(),
    )

    private fun compilation(): TrackedCompilation {
        val result = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition(), modelFile.readText())
        result.findings.forEach { println("[${it.severity}] ${it.code}: ${it.message}") }
        return result
    }

    private fun spec(): TrackedVehicleSpec = compilation().requireSpec()

    @Test
    fun theAuthoredDataAndModelHashesAreTheAuditedOnes() {
        assertEquals(
            "7b41110d6755173675df00879353486916dfbcbf58d211556c249f7f62757aec",
            sha256(definitionFile.readText()),
            "99A mechanism JSON changed; re-audit before reusing the old numbers",
        )
        assertEquals(
            "fe7099f7a850bf3b8730471065c1b74ee53cd29dc182ff86dec6abe8006bf747",
            sha256(modelFile.readText()),
            "99A model JSON changed; re-audit before reusing the old numbers",
        )
    }

    @Test
    fun theCompiledMechanismMatchesTheAuthoredWheels() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(8, compiled.wheels.size, "$side wheel count")
            assertEquals(6, compiled.wheels.count { it.isSuspended }, "$side sprung road wheels")
            assertEquals(1, compiled.wheels.count { it.receivesDrive }, "$side drive wheels")
            assertEquals(1, compiled.wheels.count { it.role == TrackWheelRole.IDLER_GUIDE }, "$side idler")
            assertTrue(
                compiled.loop.linkCount in 75..85,
                "$side link count ${compiled.loop.linkCount}",
            )
            assertTrue(
                abs(compiled.loop.closureError) < ClosedTrackLoopSolver.MAX_CLOSURE_ERROR_M,
                "$side closure ${compiled.loop.closureError}",
            )
            assertTrue(
                compiled.loop.maxPitchError < ClosedTrackLoopSolver.MAX_PITCH_ERROR_M,
                "$side pitch error ${compiled.loop.maxPitchError}",
            )
            println(
                "$side: links=${compiled.loop.linkCount} pitch=${compiled.loop.pitch} " +
                    "idlerOffset=${compiled.loop.idlerOffset} wrap=${compiled.loop.maxJointAngleDegrees} " +
                    "pinChord=${compiled.loop.minPinChordClearanceM} shoeGap=${compiled.geometry.minShoeWheelGapM}",
            )
        }
    }

    /**
     * Every authored wheel must survive the round trip, in both directions.
     *
     * The vehicle resource is the authoritative list of stations, so the audit
     * reads it directly instead of hard-coding three sampled positions: a moved
     * wheel would otherwise pass.
     */
    @Test
    fun everyAuthoredWheelCentreRadiusAndWidthSurvivesCompilation() {
        val spec = spec()
        val vehicle = JsonParser.parseString(vehicleFile.readText()).asJsonObject
        val origin = definition().chassisOrigin
        val collision = vehicle.getAsJsonArray("OBB").map { it.asJsonObject }
            .first { it.get("Part")?.asString == "Collision" }
        val collisionPosition = collision.getAsJsonArray("Position").map { it.asDouble }
        assertEquals(collisionPosition[0], origin.x, 1e-9, "ChassisOrigin.x must be the Collision OBB centre")
        assertEquals(collisionPosition[1], origin.y, 1e-9, "ChassisOrigin.y must be the Collision OBB centre")
        assertEquals(collisionPosition[2], origin.z, 1e-9, "ChassisOrigin.z must be the Collision OBB centre")

        val authored = vehicle.getAsJsonArray("Wheels").map { element ->
            val wheel = element.asJsonObject
            AuthoredWheel(
                side = TrackedSide.valueOf(wheel.get("Side").asString.uppercase()),
                role = parseRole(wheel.get("Role").asString),
                position = wheel.getAsJsonArray("Position").map { it.asDouble },
                radius = wheel.get("Radius").asDouble,
                width = wheel.get("Width").asDouble,
            )
        }
        assertEquals(16, authored.size, "99A must enumerate all stations")

        for (wheel in authored) {
            val compiled = spec.side(wheel.side).wheels.singleOrNull { candidate ->
                candidate.role == wheel.role &&
                    abs(candidate.bindCenter.y - (wheel.position[1] - origin.y)) < 2e-3 &&
                    abs(candidate.bindCenter.z - (wheel.position[2] - origin.z)) < 2e-3
            }
            assertNotNull(compiled, "no compiled ${wheel.side} ${wheel.role} wheel near ${wheel.position}")
            val centre = compiled!!
            assertTrue(
                abs(centre.bindCenter.x - (wheel.position[0] - origin.x)) < 2e-3,
                "${centre.bone} x ${centre.bindCenter.x} vs ${wheel.position[0] - origin.x}",
            )
            assertEquals(wheel.radius, centre.radius, 1e-6, "${centre.bone} radius")
            assertEquals(wheel.width, centre.halfWidth, 1e-6, "${centre.bone} half width")
        }
    }

    /** A support wheel's bind and assembly centres coincide; the idler carries the tensioner offset. */
    @Test
    fun onlyTheIdlerMovesWhenTheLoopIsTensioned() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            for (wheel in compiled.wheels) {
                if (wheel.role == TrackWheelRole.IDLER_GUIDE) {
                    assertTrue(
                        abs(wheel.assemblyCenter.z - wheel.bindCenter.z - compiled.loop.idlerOffset) < 1e-9,
                        "${wheel.bone} must carry the solved idler offset",
                    )
                } else {
                    assertEquals(wheel.bindCenter, wheel.assemblyCenter, "${wheel.bone} must not move")
                }
            }
        }
    }

    /**
     * The shoe is a solid box, so the pin chord clearance is not a shoe
     * clearance. On this vehicle the pin chord reads tens of millimetres while
     * the box clearance is a few; the physical number is the one that must be
     * positive.
     */
    @Test
    fun theShoeHasRealClearanceNotJustAPinChordClearance() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val audit = compiled.geometry
            assertTrue(audit.checkedPairs > 0, "$side audit checked no shoe/wheel pair")
            assertTrue(
                audit.minShoeWheelGapM > 0.0,
                "$side shoe/wheel gap ${audit.minShoeWheelGapM} m at link ${audit.worstLinkIndex} / " +
                    "${audit.worstWheelBone}",
            )
            assertTrue(
                audit.minPinChordClearanceM > 0.0,
                "$side pin chord clearance ${audit.minPinChordClearanceM} m",
            )
            assertTrue(
                audit.worstLateralOverlapM > 0.0,
                "$side worst pair must actually overlap laterally, or the gap is meaningless",
            )
            println(
                "$side: pinChord=${audit.minPinChordClearanceM * 1000} mm " +
                    "shoeGap=${audit.minShoeWheelGapM * 1000} mm " +
                    "at link ${audit.worstLinkIndex}/${audit.worstWheelBone}",
            )
        }
    }

    @Test
    fun theMassBudgetClosesAgainstTheAuthoredVehicleMass() {
        val spec = spec()
        assertEquals(55_000.0, spec.mass.totalMassKg, 1.0, "authored vehicle mass")
        assertEquals(
            spec.mass.totalMassKg,
            spec.mass.hullMassKg + spec.mass.runningGearMassKg,
            1e-6,
            "hull + running gear must equal the authored mass",
        )
        assertEquals(
            spec.mass.hullMassKg,
            spec.stationMassTotal,
            1.0,
            "sprung stations must carry the whole hull",
        )
        assertEquals(spec.totalLinkCount, spec.side(TrackedSide.LEFT).loop.linkCount * 2)
        assertEquals(spec.totalWheelCount, 16)
        println(
            "hull=${spec.mass.hullMassKg} gear=${spec.mass.runningGearMassKg} " +
                "links=${spec.totalLinkCount} wheels=${spec.totalWheelCount}",
        )
    }

    /** Every simulated body has an inertia derived from a shape this compiler actually builds. */
    @Test
    fun everyDynamicBodyHasATraceableInertia() {
        val spec = spec()
        assertEquals(2, spec.mass.shoes.size, "one representative shoe per side")
        for (shoe in spec.mass.shoes) {
            assertEquals(MassBasis.BOX, shoe.basis)
            assertTrue(shoe.inertiaKgM2.x > 0.0 && shoe.inertiaKgM2.y > 0.0 && shoe.inertiaKgM2.z > 0.0)
        }
        assertEquals(16, spec.mass.wheels.size)
        for (wheel in spec.mass.wheels) {
            assertEquals(MassBasis.CYLINDER, wheel.basis)
            assertTrue(wheel.inertiaKgM2.x > 0.0 && wheel.inertiaKgM2.y > 0.0 && wheel.inertiaKgM2.z > 0.0)
        }
        // A lateral axle puts the axial inertia on x: I_axis = m r^2 / 2.
        val idler = spec.side(TrackedSide.LEFT).wheels.first { it.role == TrackWheelRole.IDLER_GUIDE }
        val audited = spec.mass.wheels.first { it.name.endsWith(idler.bone) }
        assertEquals(0.5 * idler.massKg * idler.radius * idler.radius, audited.inertiaKgM2.x, 1e-9)
    }

    /**
     * Physical links, visible link templates and model bones are three different
     * numbers. The physical count is what Bullet simulates; the visible count is
     * a render template repetition; the bone count belongs to the model.
     */
    @Test
    fun physicalVisibleAndSkeletonCountsAreRecordedSeparately() {
        val spec = spec()
        val geometry = BedrockModelGeometry.parse(modelFile.readText())
        val physical = spec.totalLinkCount
        val visible = spec.renders.values.sumOf { it.visibleBones.size }
        val skeleton = geometry.bones.size

        assertEquals(spec.side(TrackedSide.LEFT).loop.linkCount, physical / 2, "physical links per side")
        assertEquals(37, spec.renders.getValue(TrackedSide.LEFT).visibleBones.size, "visible track bones per side")
        assertEquals(37, spec.renders.getValue(TrackedSide.RIGHT).visibleBones.size, "visible track bones per side")
        assertTrue(skeleton > visible, "model bones must exceed the visible track subset")
        assertEquals(287, skeleton, "99A model bone count")
        println("physical=$physical visible=$visible skeleton=$skeleton")

        assertTrue(physical != visible, "physical and visible link counts must not be assumed equal")
        // No visible pitch may be invented from the bone count.
        assertNull(spec.renders.getValue(TrackedSide.LEFT).visiblePitchM)
    }

    @Test
    fun everyWheelRoleComesFromDataNotBoneOrder() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val wheels = spec.side(side).wheels
            // The drive sprocket is at the rear on 99A and the idler at the
            // front. If the compiler guessed by index this would silently swap.
            assertEquals(TrackWheelRole.IDLER_GUIDE, wheels.first().role, "$side front wheel")
            assertEquals(TrackWheelRole.DRIVE_GUIDE, wheels.last().role, "$side rear wheel")
            assertTrue(wheels.drop(1).dropLast(1).all { it.role == TrackWheelRole.SUPPORT })
        }
    }

    /** Reordering the authored list must not change roles or bones. */
    @Test
    fun wheelOrderInTheFileDoesNotDecideRoles() {
        val shuffled = definition().also { definition ->
            definition.left.wheels.shuffle(java.util.Random(7))
            definition.right.wheels.reverse()
        }
        val spec = TrackedVehicleCompiler
            .compile("superbwarfare:ztz_99a", shuffled, modelFile.readText())
            .requireSpec()
        assertEquals(TrackWheelRole.IDLER_GUIDE, spec.side(TrackedSide.LEFT).wheels.first().role)
        assertEquals(TrackWheelRole.DRIVE_GUIDE, spec.side(TrackedSide.LEFT).wheels.last().role)
        assertEquals(TrackWheelRole.IDLER_GUIDE, spec.side(TrackedSide.RIGHT).wheels.first().role)
        assertEquals(TrackWheelRole.DRIVE_GUIDE, spec.side(TrackedSide.RIGHT).wheels.last().role)
        assertEquals(79, spec.side(TrackedSide.LEFT).loop.linkCount)
    }

    /** The belt plane comes from the template pivot, not from the wheel hubs. */
    @Test
    fun theBeltPlaneComesFromTheTemplatePivot() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val centreX = compiled.linkTemplate.centerX
            val hubMean = compiled.wheels.map { it.bindCenter.x }.average()
            assertTrue(
                abs(centreX - hubMean) > 0.05,
                "$side belt plane must not be the wheel-hub mean",
            )
            for (placement in compiled.placements) {
                assertEquals(centreX, placement.center.x, 1e-9, "link ${placement.index} lateral centre")
            }
        }
        assertEquals(1.8359375, spec.side(TrackedSide.LEFT).linkTemplate.centerX, 1e-6)
        assertEquals(-1.8359375, spec.side(TrackedSide.RIGHT).linkTemplate.centerX, 1e-6)
    }

    @Test
    fun anInvalidMechanismNeverProducesAUsableSpec() {
        val limited = definition().also { it.track.angularLimitDegrees = 20.0 }
        val result = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", limited, modelFile.readText())
        assertTrue(result.hasErrors, "a 20 degree hinge limit cannot carry the 99A wrap")
        assertNull(result.spec)
        val failure = runCatching { result.requireSpec() }.exceptionOrNull()
        assertTrue(failure is TrackedGeometryException, "requireSpec must throw, got $failure")

        val broken = definition().also { it.left.wheels.first().mass = 0.0 }
        val massResult = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", broken, modelFile.readText())
        assertTrue(massResult.hasErrors, "a zero wheel mass must be rejected")
        assertNull(massResult.spec)
    }

    @Test
    fun aTangentShoeIsReportedRatherThanHidden() {
        // With no authored clearance the corrected locus leaves the shoe exactly
        // tangent to the guide wheels. That is not an error, but it must be
        // reported: a zero-gap assembly has no margin for wheel deflection.
        val tangent = definition().also { it.track.clearance = 0.0 }
        val result = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", tangent, modelFile.readText())
        val spec = result.requireSpec()
        for (side in TrackedSide.entries) {
            val audit = spec.side(side).geometry
            assertTrue(
                audit.minShoeWheelGapM < CompiledGeometryAudit.INTERPENETRATION_TOLERANCE_M,
                "$side tangent shoe must be reported, gap ${audit.minShoeWheelGapM}",
            )
            assertFalse(audit.interpenetrates, "$side tangent shoe is not an interpenetration")
        }
        assertTrue(
            result.findings.any { it.code.endsWith("shoe-contact") },
            "a tangent shoe must produce a shoe-contact finding",
        )
    }

    private data class AuthoredWheel(
        val side: TrackedSide,
        val role: TrackWheelRole,
        val position: List<Double>,
        val radius: Double,
        val width: Double,
    )

    private fun parseRole(value: String): TrackWheelRole = when (value) {
        "Support" -> TrackWheelRole.SUPPORT
        "DriveGuide" -> TrackWheelRole.DRIVE_GUIDE
        "IdlerGuide" -> TrackWheelRole.IDLER_GUIDE
        "ReturnRoller" -> TrackWheelRole.RETURN_ROLLER
        else -> throw IllegalArgumentException("unknown wheel role $value")
    }

    private fun sha256(text: String): String = TrackedVehicleCompiler.sha256(text)
}
