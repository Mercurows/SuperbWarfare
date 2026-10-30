package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * B2.0: the measurement contract, verified before any Bullet object exists.
 *
 * This test answers exactly one question: *can the audited mechanism be
 * measured, and can a sloppy record be rejected?* It never creates a physics
 * world, never applies a force and never claims a run passed.
 *
 * The distinction it protects is the one B2 keeps tripping over: a fixture that
 * assembles its own easier geometry, or a record that omits a field, must fail
 * here rather than produce an encouraging number later.
 */
class B2RunObservationTest {
    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun spec(): TrackedVehicleSpec {
        val definition = json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        )
        val model = File(
            resources,
            "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json",
        ).readText()
        return TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model).requireSpec()
    }

    // ---------------------------------------------------------------------
    // Run topology: the assembled pose is the audited one
    // ---------------------------------------------------------------------

    /**
     * Every run must be the audited box at the audited pose.
     *
     * The probe refuses a spec list that was not produced from the compiled side,
     * which is what stops a fixture from quietly assembling an easier mechanism.
     */
    @Test
    fun theRunSpecsMatchTheAuditedMechanism() {
        val compiledSpec = spec()
        for (side in TrackedSide.entries) {
            val compiled = compiledSpec.side(side)
            val runs = TrackedRunSpecs.compileRuns(compiledSpec, side)
            TrackRunAssemblyProbe.requireMatches(runs, compiled)
            assertEquals(compiled.runs.size, runs.size, "$side run count")
            assertEquals(8, runs.size, "$side must have one run per adjacent member pair")
        }
    }

    /**
     * The assembly probe must actually compare two different body placements.
     *
     * This is the regression that matters: an earlier version computed both sides
     * of every anchor from the run alone, so the gap was identically zero and the
     * probe would "pass" no matter what the compiled frames said.
     */
    @Test
    fun theAssemblyProbeComparesTwoBodyPlacements() {
        val compiledSpec = spec()
        val compiled = compiledSpec.side(TrackedSide.LEFT)
        val runs = TrackedRunSpecs.compileRuns(compiledSpec, TrackedSide.LEFT)
        val measured = TrackRunAssemblyProbe.measure(runs, compiled)
        assertEquals(emptyList<String>(), measured.problems(), "measurement problems")
        assertTrue(measured.anchorsCoincide, "worst anchor gap ${measured.worstAnchorGapM} m")
        assertTrue(measured.axesAligned, "worst locked axis ${measured.worstLockedAxisRad} rad")
        assertEquals(compiled.runs.size, measured.runCount)
        assertEquals(compiled.wheels.size, measured.wheelCount)
        assertEquals(compiled.runs.size * 2, measured.endCount)

        // Now prove the probe is not blind: displace a wheel and the anchor gap
        // must grow. If both sides were computed from the run, this stays zero.
        val broken = compiled.wheels.map { wheel ->
            if (wheel.index == compiled.runs.first().firstWheelIndex) {
                wheel.copy(assemblyCenter = wheel.assemblyCenter.add(net.minecraft.world.phys.Vec3(0.05, 0.0, 0.0)))
            } else {
                wheel
            }
        }
        val displaced = CompiledTrackSide(
            side = compiled.side,
            wheels = broken,
            belt = compiled.belt,
            beltOrder = compiled.beltOrder,
            members = compiled.members,
            runs = compiled.runs,
            mounts = compiled.mounts,
            suspension = compiled.suspension,
            legacy = compiled.legacy,
        )
        val shifted = TrackRunAssemblyProbe.measure(runs, displaced)
        assertTrue(
            shifted.worstAnchorGapM > 0.04,
            "the probe must see a displaced wheel, gap ${shifted.worstAnchorGapM} m",
        )
        assertFalse(shifted.anchorsCoincide, "a displaced wheel must fail the assembly gate")
    }

    @Test
    fun aForeignRunListIsRejected() {
        val compiledSpec = spec()
        val compiled = compiledSpec.side(TrackedSide.LEFT)
        val runs = TrackedRunSpecs.compileRuns(compiledSpec, TrackedSide.LEFT)
        assertThrows(IllegalArgumentException::class.java) {
            TrackRunAssemblyProbe.requireMatches(runs.dropLast(1), compiled)
        }
        // A single-link run with a wrong mass must also be rejected.
        val tampered = runs.toMutableList()
        val first = tampered[0]
        tampered[0] = TrackRunSpec(
            name = first.name,
            links = listOf(first.links.single().copy(massKg = first.links.single().massKg + 10f)),
            joints = emptyList(),
            friction = first.friction,
        )
        assertThrows(IllegalArgumentException::class.java) {
            TrackRunAssemblyProbe.requireMatches(tampered, compiled)
        }
    }

    // ---------------------------------------------------------------------
    // Legacy shoe chain: history stays reproducible, but never looks current
    // ---------------------------------------------------------------------

    @Test
    fun aFixedIdlerDoesNotRebuildTheHistoricalShoeChain() {
        val compiledSpec = spec()
        for (side in TrackedSide.entries) {
            assertEquals(null, compiledSpec.side(side).legacy, "$side idler is fixed")
            val runs = TrackedRunSpecs.compileRuns(compiledSpec, side)
            TrackRunAssemblyProbe.requireMatches(runs, compiledSpec.side(side))
            assertEquals(compiledSpec.side(side).runs.size, runs.size)
        }
    }

    /** FULL density is defined by the run count, not by the historical 79 shoes. */
    @Test
    fun fullDensityIsDefinedByRunsNotShoes() {
        val compiled = spec().side(TrackedSide.LEFT)
        assertEquals(
            "full density, ${compiled.runs.size} runs",
            B2ChainDensity.FULL.describe(compiled.runs.size, compiled.runs.size),
        )
        assertThrows(IllegalArgumentException::class.java) {
            B2ChainDensity.FULL.describe(2, compiled.runs.size)
        }
        assertTrue(
            B2ChainDensity.DIAGNOSTIC.describe(2, compiled.runs.size).contains("DIAGNOSTIC"),
        )
    }

    // ---------------------------------------------------------------------
    // The observation record
    // ---------------------------------------------------------------------

    private fun record(
        substeps: Int = B2Sampling.SETTLE_SUBSTEPS,
        anchor: Double = 1e-7,
        normal: Double = 1e-4,
        slip: Double = 0.02,
        runWheelContacts: Int = 6,
        runTerrain: Int = 5,
        runRun: Int = 0,
        stations: Int = 6,
    ) = B2RunObservation(
        side = TrackedSide.LEFT,
        density = B2ChainDensity.FULL,
        substeps = substeps,
        topologyRevision = "ztz_99a/left/runs-8/belt-v1",
        endpointAnchorErrorM = anchor,
        endpointFrameAngleErrorRad = 1e-5,
        normalResidualM = normal,
        tangentialSlipM = slip,
        lockedAxisDriftRad = 1e-6,
        runWheelContactCount = runWheelContacts,
        wheelCount = 8,
        runWheelPenetrationM = 0.0,
        runTerrainContactCount = runTerrain,
        runRunContactCount = runRun,
        runDriftM = 1e-4,
        suspensionTravelM = List(stations) { 0.05 },
        suspensionForceN = List(stations) { 16_000.0 },
        chassisHeaveM = -0.001,
        chassisPitchRad = 1e-4,
    )

    @Test
    fun aCompleteRecordHasNoStructuralProblems() {
        assertEquals(emptyList<String>(), record().problems(expectedStations = 6, expectedRuns = 8))
        assertTrue(record().isFullDensity)
    }

    @Test
    fun theRecordCarriesNoFabricatedJointLoad() {
        val names = B2RunObservation::class.java.methods.map { it.name.lowercase() }
        assertTrue(
            names.none { "impulse" in it || "jointload" in it || "jointforce" in it },
            "a B2 record must not expose a fabricated joint load: $names",
        )
    }

    @Test
    fun nonFiniteMeasurementsAreRejected() {
        assertTrue(record(anchor = Double.NaN).problems(6, 8).isNotEmpty())
        assertTrue(record(normal = Double.POSITIVE_INFINITY).problems(6, 8).isNotEmpty())
        assertTrue(record(slip = Double.NaN).problems(6, 8).isNotEmpty())
    }

    @Test
    fun aBlankTopologyRevisionIsRejected() {
        val missing = record().copy(topologyRevision = "")
        assertTrue(missing.problems(6, 8).any { "topologyRevision" in it })
    }

    @Test
    fun selfContactingRunsAreRejected() {
        assertTrue(record(runRun = 3).problems(6, 8).any { "same-owner runs" in it })
    }

    @Test
    fun impossibleContactCountsAreRejected() {
        assertTrue(record(runWheelContacts = 9).problems(6, 8).isNotEmpty())
        assertTrue(record(runTerrain = 9).problems(6, 8).isNotEmpty())
        assertTrue(record(runTerrain = -1).problems(6, 8).isNotEmpty())
    }

    @Test
    fun mismatchedStationCountsAreRejected() {
        assertTrue(record(stations = 5).problems(6, 8).isNotEmpty())
    }

    @Test
    fun signErrorsInDerivedMetricsAreRejected() {
        assertTrue(record(anchor = -1e-3).problems(6, 8).isNotEmpty())
        assertTrue(record(normal = -1e-3).problems(6, 8).isNotEmpty())
        assertTrue(record(slip = -1e-3).problems(6, 8).isNotEmpty())
        assertTrue(record().copy(runDriftM = -1.0).problems(6, 8).isNotEmpty())
        assertTrue(record().copy(runWheelPenetrationM = -1.0).problems(6, 8).isNotEmpty())
    }

    @Test
    fun theSamplingWindowsAreFixed() {
        assertEquals(1.0 / 60.0, B2Sampling.SUBSTEP_SECONDS, 0.0)
        assertEquals(300, B2Sampling.SHORT_CHAIN_SUBSTEPS)
        assertEquals(600, B2Sampling.SETTLE_SUBSTEPS)
    }

    @Test
    fun theDiagnosticsLineCarriesNoVerdict() {
        val line = record().describe().lowercase()
        listOf("pass", "fail", "ok", "good", "stable").forEach { word ->
            assertFalse(Regex("\\b$word\\b").containsMatchIn(line), "diagnostics line claims '$word': $line")
        }
        assertTrue(line.contains("anchor="), "diagnostics must report the anchor residual")
    }

    @Test
    fun bothSidesAreMeasurable() {
        val compiledSpec = spec()
        for (side in TrackedSide.entries) {
            val runs = TrackedRunSpecs.compileRuns(compiledSpec, side)
            val measured = TrackRunAssemblyProbe.measure(runs, compiledSpec.side(side))
            assertEquals(emptyList<String>(), measured.problems(), "$side problems")
            val box = runs.first().links.single().shape as TrackRunLinkShape.Box
            assertTrue(box.halfExtents.x > 0.0 && box.halfExtents.y > 0.0 && box.halfExtents.z > 0.0)
        }
    }
}
