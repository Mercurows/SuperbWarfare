package com.atsuishio.superbwarfare.physics.tracked

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * Compiles the real ZTZ-99A asset through the generic tracked mechanism layer.
 *
 * This is intentionally a plain JVM test: the mechanism contract must be
 * verifiable without a Minecraft runtime, so a geometry or mass regression can
 * never hide behind a game test that takes minutes to boot.
 *
 * The numbers asserted here are *candidate* values taken from the authored
 * asset. They are not an approval: they exist to detect silent drift between
 * the model and the compiled mechanism.
 */
class TrackedVehicleCompilationTest {

    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun ztz99aCompilesToAClosedMechanism() {
        val definition = json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        )
        val model = File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText()

        val spec = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model)


        println("=== ZTZ-99A mechanism audit ===")
        spec.audit.forEach { println("[${it.severity}] ${it.code}: ${it.message}") }
        println("modelSha256=${spec.modelSha256}")
        println("totalWheelCount=${spec.totalWheelCount} totalLinkCount=${spec.totalLinkCount} " +
            "stationMassTotal=${spec.stationMassTotal}")

        assertFalse(spec.hasErrors, "99A must compile without mechanism errors")

        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            assertEquals(8, compiled.wheels.size, "$side wheel count")
            assertEquals(6, compiled.wheels.count { it.isSuspended }, "$side sprung road wheels")
            assertEquals(1, compiled.wheels.count { it.receivesDrive }, "$side drive wheels")
            assertTrue(compiled.loop.linkCount in 70..90, "$side link count ${compiled.loop.linkCount}")
            assertTrue(
                abs(compiled.loop.closureError) < ClosedTrackLoopSolver.MAX_CLOSURE_ERROR_M,
                "$side closure ${compiled.loop.closureError}",
            )
            assertTrue(
                compiled.loop.maxJointAngleDegrees <= definition.track.angularLimitDegrees,
                "$side wrap ${compiled.loop.maxJointAngleDegrees}",
            )
            println(
                "$side: links=${compiled.loop.linkCount} length=${compiled.loop.totalLength} " +
                    "idlerOffset=${compiled.loop.idlerOffset} wrap=${compiled.loop.maxJointAngleDegrees} " +
                    "clearance=${compiled.loop.minWheelClearance}",
            )
            compiled.wheels.forEach {
                println(
                    "  ${it.index} ${it.role} ${it.bone} centre=${it.center} " +
                        "r=${it.radius} halfW=${it.halfWidth} mass=${it.suspension.stationMassKg}",
                )
            }
        }

        // The compiled geometry must reproduce the authored wheel centres. A
        // mismatch here means the model-to-physics basis or the pivot chain is
        // wrong, which would silently misplace every body.
        val left = spec.side(TrackedSide.LEFT)
        val byBone = left.wheels.associateBy { it.bone }
        fun checkCentre(bone: String, x: Double, y: Double, z: Double) {
            val wheel = byBone.getValue(bone)
            assertTrue(abs(wheel.center.x - x) < 2e-3, "$bone x ${wheel.center.x} vs $x")
            assertTrue(abs(wheel.center.y - y) < 2e-3, "$bone y ${wheel.center.y} vs $y")
            assertTrue(abs(wheel.center.z - z) < 2e-3, "$bone z ${wheel.center.z} vs $z")
        }
        // Authored values from data/superbwarfare/sbw/vehicles/ztz_99a.json.
        checkCentre("wheelL0", 1.840821, 1.113281, 3.844844)
        checkCentre("wheelL1", 2.055664, 0.585938, 2.858516)
        checkCentre("wheelL7", 1.840821, 1.113281, -3.811406)

        // Radii are derived from the model, not copied from the data file. Keep
        // the assertion wide enough to accept a legitimate model revision but
        // narrow enough to catch a wrong subtree being measured.
        assertTrue(byBone.getValue("wheelL1").radius in 0.45..0.60, "road wheel radius")
        assertTrue(byBone.getValue("wheelL0").radius in 0.30..0.42, "idler radius")

        assertEquals(55_000.0, spec.stationMassTotal + spec.runningGearMassKg, 1.0)
        println("sprung=${spec.stationMassTotal} gear=${spec.runningGearMassKg}")
    }
}
