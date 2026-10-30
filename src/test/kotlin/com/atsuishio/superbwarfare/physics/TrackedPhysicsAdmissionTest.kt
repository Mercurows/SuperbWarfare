package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleCompiler
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleDefinition
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class TrackedPhysicsAdmissionTest {
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
    fun aMissingSpecIsRefused() {
        val reason = TrackedPhysicsAdmission.rejectReason(null, null)
        assertTrue(reason?.contains("compiled") == true, reason)
    }

    @Test
    fun anInvalidMassPlanIsRefused() {
        val plan = VehicleMassPlan(
            totalMassKg = 55_000f,
            chassisMassKg = 1f,
            turretMassKg = 0f,
            barrelMassKg = 0f,
            wheelMassKg = 0f,
            runMassKg = 0f,
            source = "test",
            problems = listOf("authored mass does not match the compiled mechanism"),
        )
        val reason = TrackedPhysicsAdmission.rejectReason(spec(), plan)
        assertTrue(reason?.contains("authored mass") == true, reason)
    }

    @Test
    fun aClosingPlanIsAdmitted() {
        val compiled = spec()
        val plan = VehicleMassPlan.tracked(
            totalMassKg = compiled.massKg.toFloat(),
            spec = compiled,
            settings = VehiclePhysicsSettings(),
            anchored = false,
            turret = null,
            barrel = null,
        )
        assertNull(TrackedPhysicsAdmission.rejectReason(compiled, plan))
    }
}
