package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleCompiler
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import kotlinx.serialization.json.Json
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * Mass option A must reach the bodies that actually exist.
 *
 * The compiler already proves the budget closes on paper. This test proves the
 * production plan consumes that budget instead of leaving the hull at the full
 * authored mass and then creating extra running-gear bodies on top.
 */
class VehicleMassPlanTest {

    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun spec() = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        ),
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
    ).requireSpec()

    @Test
    fun optionAClosesWithoutDoubleCountingRunningGear() {
        val compiled = spec()
        val plan = VehicleMassPlan.tracked(
            totalMassKg = 55_000f,
            spec = compiled,
            settings = VehiclePhysicsSettings(),
            anchored = false,
            turret = null,
            barrel = null,
        )
        assertTrue(plan.problems.isEmpty(), "plan problems: ${plan.problems}")
        assertTrue(plan.closes(), "allocated ${plan.allocatedMassKg} != authored ${plan.totalMassKg}")
        assertEquals(12_400f, plan.wheelMassKg, 1f, "wheel ends keep authored mass")
        assertEquals(2_844f, plan.runMassKg, 1f, "run budget is the old shoe budget, both sides")
        assertEquals(39_756f, plan.chassisMassKg, 1f, "hull is the remaining sprung assembly")
        assertEquals(0f, plan.turretMassKg, 0f)
        assertEquals(0f, plan.barrelMassKg, 0f)
        assertTrue(abs(plan.allocatedMassKg - 55_000f) <= VehicleMassPlan.TOLERANCE_KG)
    }

    @Test
    fun articulationIsTakenFromTheSprungAssemblyNotAddedOnTop() {
        val compiled = spec()
        val turret = ArticulationSpec(
            pivot = Vec3.ZERO,
            parts = listOf(CollisionPart(Vec3(0.5, 0.5, 0.5), Vec3.ZERO, Quaternionf())),
            axis = 1,
            lowerLimit = 0.0,
            upperLimit = 0.0,
        )
        val plan = VehicleMassPlan.tracked(
            totalMassKg = 55_000f,
            spec = compiled,
            settings = VehiclePhysicsSettings(articulationMassFraction = 0.03f),
            anchored = false,
            turret = turret,
            barrel = null,
        )
        assertTrue(plan.valid, "plan problems: ${plan.problems}")
        assertTrue(plan.turretMassKg > 0f, "a turret must take a share of the sprung assembly")
        assertEquals(0f, plan.barrelMassKg, 0f)
        assertEquals(
            39_756f,
            plan.chassisMassKg + plan.turretMassKg,
            1f,
            "chassis + turret must still equal the sprung assembly",
        )
        assertTrue(abs(plan.allocatedMassKg - 55_000f) <= VehicleMassPlan.TOLERANCE_KG)
    }

    @Test
    fun aMismatchedAuthoredMassIsReported() {
        val compiled = spec()
        val plan = VehicleMassPlan.tracked(
            totalMassKg = 40_000f,
            spec = compiled,
            settings = VehiclePhysicsSettings(),
            anchored = false,
            turret = null,
            barrel = null,
        )
        assertTrue(plan.problems.any { it.contains("differs") }, "a 15 t mismatch must be reported")
    }
}
