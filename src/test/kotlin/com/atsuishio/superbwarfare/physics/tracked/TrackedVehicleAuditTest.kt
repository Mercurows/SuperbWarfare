package com.atsuishio.superbwarfare.physics.tracked

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import net.minecraft.world.phys.Vec3
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
    fun theTerrainWindowContainsTheCompiledRunningGearEnvelope() {
        val spec = spec()
        val envelope = TerrainEnvelope.of(spec)
        val window = TrackedTerrainSampler.window(Vec3.ZERO, envelope, spec.terrain)
        assertTrue(window.minX <= envelope.minX && window.maxX >= envelope.maxX)
        assertTrue(window.minY <= envelope.minY && window.maxY >= envelope.maxY)
        assertTrue(window.minZ <= envelope.minZ && window.maxZ >= envelope.maxZ)
    }

    @Test
    fun theAuthoredDataAndModelHashesAreTheAuditedOnes() {
        assertEquals(
            "f375e73ba3376eb405523db05cdf4219fac5c3235575ed3226db042387c81e72",
            sha256(definitionFile.readText()),
            "99A mechanism JSON changed; re-audit before reusing the old numbers",
        )
        assertEquals(
            "5809b45cc3bd3121ce439aaa6cab5e7113623f18ef30d849692205c90f1df369",
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
            assertEquals(8, compiled.runs.size, "$side run count")
            assertEquals(null, compiled.legacy, "$side fixed idler must not rebuild the shoe chain")
            val idler = compiled.wheels.single { it.role == TrackWheelRole.IDLER_GUIDE }
            val mount = compiled.mount(idler.index)
            assertEquals(WheelMountKind.FIXED, mount.kind, "${idler.bone} must be fixed")
            assertFalse(mount.isSprung, "${idler.bone} must not be suspended")
            assertFalse(mount.isTensioned, "${idler.bone} must not be tensioned")
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

    /**
     * The run topology keeps every wheel at its authored bind centre.
     *
     * The legacy shoe chain used to move the idler by its solved tensioner
     * offset; the run mechanism must not inherit that, because the idler's free
     * travel is a joint coordinate of the mount, not a new bind position.
     */
    @Test
    fun theRunTopologyKeepsAuthoredBindCentres() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            for (wheel in compiled.wheels) {
                assertEquals(wheel.bindCenter, wheel.assemblyCenter, "${wheel.bone} must stay at its bind centre")
            }
            assertEquals(null, compiled.legacy, "$side idler is fixed, so the shoe chain is absent")
            assertEquals(
                8,
                compiled.runs.size,
                "$side run count must come from the belt order, not the shoe chain",
            )
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
            val audit = TrackedRunGeometryAudit.audit(compiled.runs, compiled.wheels, compiled.belt)
            assertTrue(audit.checkedPairs > 0, "$side audit checked no run/wheel pair")
            assertTrue(
                audit.minRunWheelGapM >= -TrackedRunGeometryAudit.CONTACT_TOLERANCE_M,
                "$side run/wheel gap ${audit.minRunWheelGapM} m at run ${audit.worstRunIndex} / " +
                    "${audit.worstWheelBone}",
            )
            assertTrue(
                audit.worstLateralOverlapM > 0.0,
                "$side worst pair must actually overlap laterally, or the gap is meaningless",
            )
        }
    }

    @Test
    fun theMassBudgetClosesAgainstTheAuthoredVehicleMass() {
        val spec = spec()
        assertEquals(55_000.0, spec.mass.totalMassKg, 1.0, "authored vehicle mass")
        assertEquals(
            spec.mass.totalMassKg,
            spec.mass.sprungAssemblyMassKg + spec.mass.runningGearMassKg,
            1e-6,
            "sprung assembly + running gear must equal the authored mass",
        )
        assertEquals(
            spec.mass.sprungAssemblyMassKg,
            spec.stationMassTotal,
            1.0,
            "sprung stations must carry the whole sprung assembly",
        )
        assertEquals(spec.totalRunCount, spec.side(TrackedSide.LEFT).runs.size * 2)
        assertEquals(spec.totalWheelCount, 16)
        println(
            "sprung=${spec.mass.sprungAssemblyMassKg} gear=${spec.mass.runningGearMassKg} " +
                "runs=${spec.totalRunCount} wheels=${spec.totalWheelCount}",
        )
    }

    /** Every simulated body has an inertia derived from a shape this compiler actually builds. */
    @Test
    fun everyDynamicBodyHasATraceableInertia() {
        val spec = spec()
        assertTrue(spec.mass.shoes.isEmpty(), "a fixed idler does not build historical shoes")
        assertEquals(16, spec.mass.runs.size, "one run body per adjacent pair, both sides")
        for (run in spec.mass.runs) {
            assertEquals(MassBasis.BOX, run.basis)
            assertTrue(run.inertiaKgM2.x > 0.0 && run.inertiaKgM2.y > 0.0 && run.inertiaKgM2.z > 0.0)
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
        val physical = spec.totalRunCount
        val visible = spec.renders.values.sumOf { it.visibleBones.size }
        val skeleton = geometry.bones.size

        assertEquals(8, spec.side(TrackedSide.LEFT).runs.size, "physical runs per side")
        assertEquals(16, physical, "physical runs, both sides")
        assertEquals(37, spec.renders.getValue(TrackedSide.LEFT).visibleBones.size, "visible track bones per side")
        assertEquals(37, spec.renders.getValue(TrackedSide.RIGHT).visibleBones.size, "visible track bones per side")
        assertTrue(skeleton > visible, "model bones must exceed the visible track subset")
        assertEquals(287, skeleton, "99A model bone count")
        println("physical=$physical visible=$visible skeleton=$skeleton")

        assertTrue(physical != visible, "physical and visible link counts must not be assumed equal")
        val pitch = spec.renders.getValue(TrackedSide.LEFT).visiblePitchM
        assertNotNull(pitch, "visible pitch comes from the template cube, not from the bone count")
        assertEquals(3.72912 / 16.0, pitch!!, 1e-6)
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
        assertEquals(8, spec.side(TrackedSide.LEFT).runs.size)
        assertEquals(WheelMountKind.FIXED, spec.side(TrackedSide.LEFT).mount(
            spec.side(TrackedSide.LEFT).wheels.single { it.role == TrackWheelRole.IDLER_GUIDE }.index,
        ).kind)
    }

    /** The belt plane comes from the template pivot, not from the wheel hubs. */
    @Test
    fun theBeltPlaneComesFromTheTemplatePivot() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val centreX = compiled.belt.centerPlaneX
            val hubMean = compiled.wheels.map { it.bindCenter.x }.average()
            assertTrue(
                abs(centreX - hubMean) > 0.05,
                "$side belt plane must not be the wheel-hub mean",
            )
            for (run in compiled.runs) {
                assertEquals(centreX, run.center.x, 1e-9, "run ${run.index} lateral centre")
            }
        }
        assertEquals(1.8359375, spec.side(TrackedSide.LEFT).belt.centerPlaneX, 1e-4)
        assertEquals(-1.8359375, spec.side(TrackedSide.RIGHT).belt.centerPlaneX, 1e-4)
    }

    @Test
    fun anInvalidMechanismNeverProducesAUsableSpec() {
        val broken = definition().also { it.left.wheels.first().mass = 0.0 }
        val massResult = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", broken, modelFile.readText())
        assertTrue(massResult.hasErrors, "a zero wheel mass must be rejected")
        assertNull(massResult.spec)
        val failure = runCatching { massResult.requireSpec() }.exceptionOrNull()
        assertTrue(failure is TrackedGeometryException, "requireSpec must throw, got $failure")

        // The run topology has no hinge limit, so a tight AngularLimitDegrees no
        // longer invalidates the mechanism: it only affects the legacy shoe audit.
        val limited = definition().also { it.track.angularLimitDegrees = 20.0 }
        val limitedResult = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", limited, modelFile.readText())
        assertNotNull(limitedResult.spec, "the run topology must not depend on the shoe hinge limit")
    }

    @Test
    fun aZeroClearanceDoesNotMoveTheFixedIdler() {
        // Clearance only affected the historical shoe locus. With a fixed idler
        // the run topology must still compile, and the idler bind pose must not
        // be shifted to invent take-up.
        val tangent = definition().also { it.track.clearance = 0.0 }
        val result = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", tangent, modelFile.readText())
        val spec = result.requireSpec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(null, compiled.legacy, "$side must not rebuild a shoe chain to spend clearance")
            val idler = compiled.wheels.single { it.role == TrackWheelRole.IDLER_GUIDE }
            assertEquals(idler.bindCenter, idler.assemblyCenter, "${idler.bone} stays on the bind pose")
            val audit = TrackedRunGeometryAudit.audit(compiled.runs, compiled.wheels, compiled.belt)
            assertFalse(audit.interpenetrates, "$side runs must still clear the wheels")
        }
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
