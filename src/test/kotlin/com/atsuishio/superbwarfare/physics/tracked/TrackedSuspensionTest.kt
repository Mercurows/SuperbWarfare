package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.PhysicsSuspensionRow
import cn.solarmoon.spark_core.physics.SuspensionRowState
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B1: independent road-wheel suspension, audited as data.
 *
 * S1 proved the mechanism geometry and mass budget. B1 has to prove that each
 * road wheel gets its own spring row, that the row carries exactly that
 * station's share of the hull, and that the travel, force and bilateral recovery
 * limits are real rather than nominal. No Bullet object is created here.
 *
 * The authoritative definition is SuperbWarfare's own suspension block; MCSP is
 * only a behavioural reference and contributes no fields to this schema.
 */
class TrackedSuspensionTest {
    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun definition() = json.decodeFromString(
        TrackedVehicleDefinition.serializer(),
        File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
    )

    private fun model() =
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText()

    private fun spec(): TrackedVehicleSpec =
        TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition(), model()).requireSpec()

    @Test
    fun everyRoadWheelGetsItsOwnRow() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            assertEquals(6, audit.stationCount, "$side must spring exactly its six road wheels")
            assertEquals(6, audit.stations.map { it.bone }.toSet().size, "$side rows must be distinct bodies")
            assertTrue(
                audit.stations.none { it.bone.startsWith("wheel${side.name.first()}0") },
                "$side idler must not be sprung",
            )
            assertTrue(
                audit.stations.none { it.bone.startsWith("wheel${side.name.first()}7") },
                "$side drive sprocket must not be sprung",
            )
        }
    }

    /** The rows must carry the whole hull, no more and no less. */
    @Test
    fun theRowsCarryExactlyTheHullMass() {
        val spec = spec()
        val total = TrackedSide.entries.sumOf { side ->
            TrackedSuspensionCompiler.compile(spec, side).sprungMassKg
        }
        assertEquals(spec.mass.hullMassKg, total, 1e-6, "summed rows must equal the hull mass")
        assertEquals(spec.stationMassTotal, total, 1e-6, "rows must match the S1 station total")

        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            assertEquals(
                audit.sprungMassKg * CompiledSuspensionStation.GRAVITY,
                audit.staticLoadTotalN,
                1e-6,
                "$side static load must be the row mass times gravity",
            )
        }
    }

    @Test
    fun eachRowIsStatisticallyConsistentWithItsOwnShare() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            for (station in audit.stations) {
                // k = m g / sag and sag = g / omega^2 must agree with the same row.
                assertEquals(
                    station.stationMassKg * CompiledSuspensionStation.GRAVITY / station.stiffnessNPerM,
                    station.staticSagM,
                    1e-9,
                    "${station.bone} static sag must follow its own spring and mass",
                )
                // The authored 2.2 Hz must survive the compile.
                assertEquals(2.2, station.naturalFrequencyHz, 1e-9, "${station.bone} frequency")
                assertEquals(0.65, station.compressionDampingRatio, 1e-9, "${station.bone} compression ratio")
                assertEquals(0.45, station.reboundDampingRatio, 1e-9, "${station.bone} rebound ratio")
            }
        }
    }

    @Test
    fun theStaticSettleIsReachableAndOffTheBumpStop() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            assertTrue(audit.staticSagFitsTravel, "$side static sag exceeds compression travel")
            assertTrue(audit.staticSagClearsBumpStop, "$side static sag sits on the bump stop")
            assertTrue(audit.minForceMargin > 1.0, "$side row cap cannot carry its share")
            println(
                "$side suspension: ${audit.stationCount} rows, sprung ${audit.sprungMassKg} kg, " +
                    "max sag ${"%.4f".format(audit.maxStaticSagM)} m, " +
                    "min force margin ${"%.2f".format(audit.minForceMargin)}x",
            )
        }
    }

    /** Every station must actually pull at full droop, not clamp to zero. */
    @Test
    fun everyRowRecoversInDroop() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            assertTrue(audit.bilateralRecovery, "$side rows must be bilateral")
            for (station in audit.stations) {
                val row = station.toRowSpec()
                val droop = PhysicsSuspensionRow.droopRestoringForceN(row)
                assertTrue(droop < 0f, "${station.bone} must pull in droop, got $droop N")
                assertTrue(
                    PhysicsSuspensionRow.hasSymmetricForceCap(row),
                    "${station.bone} force cap must be symmetric",
                )
            }
        }
    }

    /** A station's compiled bump stop must reach the core row, not a constant. */
    @Test
    fun theBumpStopShapeIsTakenFromTheAuthoredRow() {
        val definition = definition()
        definition.left.suspension.bumpStopStart = 0.5
        definition.left.suspension.bumpStopExponent = 3.0
        val compiled = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model()).requireSpec()
        val audit = TrackedSuspensionCompiler.compile(compiled, TrackedSide.LEFT)
        for (station in audit.stations) {
            assertEquals(0.5, station.bumpStopStart, 1e-9, "${station.bone} bump stop start")
            assertEquals(3.0, station.bumpStopExponent, 1e-9, "${station.bone} bump stop exponent")
            assertEquals(station.travelUpM * 0.5, station.bumpStopTravelM, 1e-9)
            assertEquals(0.5f, station.toRowSpec().bumpStopStart, 1e-6f)
            assertEquals(3f, station.toRowSpec().bumpStopExponent, 1e-6f)
        }
        // The audit's "clears the bump stop" check must follow the authored knee.
        assertEquals(
            audit.stations.all { it.staticSagM < it.bumpStopTravelM },
            audit.staticSagClearsBumpStop,
        )
    }

    /** The row primitive must receive the exact compiled values, not a copy. */
    @Test
    fun theRowSpecIsAFaithfulAdaptation() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            for (station in audit.stations) {
                val row = station.toRowSpec()
                assertEquals(station.stationMassKg.toFloat(), row.stationMassKg, 1e-3f, station.bone)
                assertEquals(station.travelUpM.toFloat(), row.travelUpM, 1e-6f, station.bone)
                assertEquals(station.travelDownM.toFloat(), row.travelDownM, 1e-6f, station.bone)
                assertEquals(station.stiffnessNPerM.toFloat(), row.stiffnessNPerM, 1e-3f, station.bone)
                assertEquals(
                    station.compressionDampingNsPerM.toFloat(),
                    row.compressionDampingNsPerM,
                    1e-3f,
                    station.bone,
                )
                assertEquals(
                    station.reboundDampingNsPerM.toFloat(),
                    row.reboundDampingNsPerM,
                    1e-3f,
                    station.bone,
                )
                assertEquals(station.maxForceN.toFloat(), row.maxForceN, 1e-3f, station.bone)
                assertTrue(row.isUsable, "${station.bone} must produce a usable core row")
            }
        }
    }

    /**
     * A row whose sag would exceed its travel is a real data error: the wheel
     * would rest on the bump stop instead of the spring. The compiler must
     * refuse the mechanism, not hand out a spec that silently sits on its stop.
     */
    @Test
    fun anOverSoftRowIsRejectedAtCompileTime() {
        val definition = definition()
        // 0.5 Hz needs 0.99 m of sag to carry the same mass.
        definition.left.suspension.frequency = 0.5
        val result = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model())

        assertTrue(result.hasErrors, "an unreachable static sag must be an error")
        assertNull(result.spec, "no specification may be produced")
        assertTrue(
            result.findings.any { it.code.endsWith("suspension.sag") },
            "the finding must name the suspension sag: ${result.findings.map { it.code }}",
        )
        assertTrue(
            runCatching { result.requireSpec() }.exceptionOrNull() is TrackedGeometryException,
            "requireSpec must refuse the mechanism",
        )
    }

    /**
     * Bind-pose frames must coincide. An anchor offset by the travel range would
     * make zero travel mean "fully compressed" and silently shift every row.
     */
    @Test
    fun theConstraintFramesCoincideAtTheBindPose() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            assertEquals(emptyList<String>(), audit.misalignedPivots, "$side pivots must coincide")
            for (station in audit.stations) {
                assertTrue(station.pivotsCoincide, "${station.bone} pivots")
                assertEquals(station.wheelCenter, station.chassisPivot, "${station.bone} chassis pivot")
                assertEquals(0.0, station.wheelPivot.length(), 1e-12, "${station.bone} wheel pivot")
            }
        }
    }

    /** Travel, axle and spring axes must stay independent. */
    @Test
    fun travelAxleAndSpringAreOrthogonal() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val audit = TrackedSuspensionCompiler.compile(spec, side)
            for (station in audit.stations) {
                assertEquals(0.0, station.axle.dot(station.direction), 1e-9, "${station.bone} axle vs travel")
                assertEquals(1.0, station.axle.length(), 1e-9, "${station.bone} axle unit")
                assertEquals(1.0, station.direction.length(), 1e-9, "${station.bone} travel unit")
            }
            assertEquals(emptyList<String>(), audit.misalignedAxles, "$side axles must be lateral")
            assertEquals(emptyList<String>(), audit.cantedDirections, "$side travel must be vertical")
        }
    }

    /** Rows must not share a spring: each gets the stiffness its own share needs. */
    @Test
    fun rowsAreIndependentAndScaleWithTheirShare() {
        val spec = spec()
        val definition = definition()
        // Front road wheel carries twice the share of the others.
        definition.left.wheels[1].loadShare = 2.0
        val modified = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model()).requireSpec()
        val audit = TrackedSuspensionCompiler.compile(modified, TrackedSide.LEFT)
        val heavy = audit.stations.first { it.bone == "wheelL1" }
        val light = audit.stations.first { it.bone == "wheelL2" }
        assertTrue(heavy.stationMassKg > light.stationMassKg, "load share must reach the row mass")
        assertTrue(heavy.stiffnessNPerM > light.stiffnessNPerM, "a heavier station needs a stiffer spring")
        // Equal sag is the point of independent rows: same frequency, same sag.
        assertEquals(heavy.staticSagM, light.staticSagM, 1e-9)
        assertEquals(
            audit.sprungMassKg,
            spec.side(TrackedSide.LEFT).wheels
                .filter { it.isSuspended }.sumOf { it.suspension.stationMassKg },
            1e-6,
        )
    }

    /** The per-wheel override must reach the row, including the bump stop. */
    @Test
    fun aWheelOverrideReplacesOnlyThatStation() {
        val definition = definition()
        definition.left.wheels[1].suspension = TrackSuspensionOverride(
            frequency = 3.0,
            compressionDampingRatio = 0.8,
            bumpStopStart = 0.6,
        )
        val compiled = TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model()).requireSpec()
        val audit = TrackedSuspensionCompiler.compile(compiled, TrackedSide.LEFT)
        val overridden = audit.stations.first { it.bone == "wheelL1" }
        val untouched = audit.stations.first { it.bone == "wheelL2" }
        assertEquals(3.0, overridden.naturalFrequencyHz, 1e-9)
        assertEquals(0.8, overridden.compressionDampingRatio, 1e-9)
        assertEquals(0.6, overridden.bumpStopStart, 1e-9)
        assertEquals(2.2, untouched.naturalFrequencyHz, 1e-9)
        assertEquals(0.65, untouched.compressionDampingRatio, 1e-9)
        assertEquals(0.85, untouched.bumpStopStart, 1e-9)
        assertTrue(
            abs(overridden.stiffnessNPerM - untouched.stiffnessNPerM) > 1.0,
            "the override must change the spring, not just the audit label",
        )
    }
}
