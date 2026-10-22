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
    // The assembled pose is the audited one
    // ---------------------------------------------------------------------

    /**
     * The audited bind pose must already satisfy the hinge invariant: both pins
     * of a joint land on the same world point. If this fails, no amount of
     * solver tuning helps, because the chain would start pre-stressed.
     */
    @Test
    fun theAuditedAssemblyAlreadyHasCoincidentPins() {
        val compiled = spec()
        for (side in TrackedSide.entries) {
            val run = TrackedRunSpecs.compile(compiled, side)
            val measured = B2AssemblyProbe.measure(run, compiled.side(side))
            assertEquals(emptyList<String>(), measured.problems(), "$side measurement problems")
            assertTrue(measured.pinCoincides, "$side worst pin gap ${measured.worstPinGapM} m")
            assertTrue(
                measured.worstPinGapM <= B2Sampling.INITIAL_PIN_TOLERANCE_M,
                "$side pin gap ${measured.worstPinGapM} m exceeds the assembly tolerance",
            )
            assertEquals(compiled.side(side).placements.size, measured.linkCount)
            assertEquals(measured.linkCount, measured.jointCount, "$side must have one hinge per link")
        }
    }

    /**
     * Neighbouring links on a curved section sit at different orientations, so
     * their world hinge axes must still agree. This is the locked part of the
     * constraint and the first place a wrong frame shows up.
     */
    @Test
    fun theAuditedAssemblyHasAlignedLockedAxes() {
        val compiled = spec()
        for (side in TrackedSide.entries) {
            val run = TrackedRunSpecs.compile(compiled, side)
            val measured = B2AssemblyProbe.measure(run, compiled.side(side))
            assertTrue(
                measured.framesAligned,
                "$side worst locked-axis misalignment ${measured.worstFrameAngleRad} rad " +
                    "at joint ${measured.worstFrameJoint}",
            )
        }
    }

    /** Structural numbers must be reported for every side, not only the first one measured. */
    @Test
    fun bothSidesAreMeasurable() {
        val compiled = spec()
        val measured = TrackedSide.entries.map { side ->
            B2AssemblyProbe.measure(TrackedRunSpecs.compile(compiled, side), compiled.side(side))
        }
        assertEquals(2, measured.size)
        assertEquals(TrackedSide.LEFT, measured[0].side)
        assertEquals(TrackedSide.RIGHT, measured[1].side)
        assertTrue(measured.all { it.linkCount == 79 }, "the 99A side must be the audited 79 links")
        assertTrue(measured.all { it.jointCount == 79 })
    }

    // ---------------------------------------------------------------------
    // The spec cannot be swapped for an easier one
    // ---------------------------------------------------------------------

    /**
     * A reduced chain is a legitimate diagnostic, but it must not be measurable
     * through this contract as if it were the audited run. Dropping links is the
     * easiest way to make an unstable mechanism look fine.
     *
     * The reduced spec here is built as an open chain from the audited shoe mass
     * and shape: a truncated closed loop is not even a valid `TrackRunSpec`,
     * because the last link would lose its hinge. That the core rejects the
     * truncated loop is itself worth knowing, so the test builds a genuinely
     * valid short chain and checks the probe still refuses it.
     */
    @Test
    fun aReducedChainIsRejectedByTheProbe() {
        val compiled = spec()
        val side = TrackedSide.LEFT
        val audited = TrackedRunSpecs.compile(compiled, side)
        val reduced = shortOpenChain(audited, linkCount = 6)
        assertTrue(reduced.problems().isEmpty(), "the diagnostic chain must still be a valid spec")

        val error = assertThrows(IllegalArgumentException::class.java) {
            B2AssemblyProbe.requireMatches(reduced, compiled.side(side))
        }
        assertTrue("6 links" in error.message.orEmpty(), error.message.orEmpty())
    }

    /** A truncated closed loop is not a valid spec at all; the core already refuses it. */
    @Test
    fun aTruncatedClosedLoopIsNotAValidSpec() {
        val audited = TrackedRunSpecs.compile(spec(), TrackedSide.LEFT)
        // The constructor validates eagerly, so the rejection happens while
        // building the truncated spec rather than on a later `problems()` call.
        val error = assertThrows(IllegalArgumentException::class.java) {
            TrackRunSpec(
                name = audited.name,
                links = audited.links.dropLast(1),
                joints = audited.joints.dropLast(1),
                friction = audited.friction,
            )
        }
        assertTrue("out of range" in error.message.orEmpty(), error.message.orEmpty())
    }

    /** A heavier shoe would change the load path, so it cannot pass as the audited one. */
    @Test
    fun aDifferentLinkMassIsRejectedByTheProbe() {
        val compiled = spec()
        val side = TrackedSide.LEFT
        val audited = TrackedRunSpecs.compile(compiled, side)
        val heavier = TrackRunSpec(
            name = audited.name,
            links = audited.links.map { it.copy(massKg = it.massKg * 2f) },
            joints = audited.joints,
            friction = audited.friction,
        )
        assertThrows(IllegalArgumentException::class.java) {
            B2AssemblyProbe.requireMatches(heavier, compiled.side(side))
        }
    }

    /** A bigger shoe would hide an interference the audit already rejected. */
    @Test
    fun aDifferentLinkShapeIsRejectedByTheProbe() {
        val compiled = spec()
        val side = TrackedSide.LEFT
        val audited = TrackedRunSpecs.compile(compiled, side)
        val fattened = TrackRunSpec(
            name = audited.name,
            links = audited.links.map { link ->
                val box = link.shape as TrackRunLinkShape.Box
                link.copy(
                    shape = TrackRunLinkShape.Box(box.halfExtents.scale(1.5)),
                )
            },
            joints = audited.joints,
            friction = audited.friction,
        )
        assertThrows(IllegalArgumentException::class.java) {
            B2AssemblyProbe.requireMatches(fattened, compiled.side(side))
        }
    }

    /**
     * A moved link means the fixture invented a pose instead of reusing
     * `TrackedAssemblyGeometry`. That is the defect S1 already fixed once.
     */
    @Test
    fun aMovedLinkIsRejectedByTheProbe() {
        val compiled = spec()
        val side = TrackedSide.LEFT
        val audited = TrackedRunSpecs.compile(compiled, side)
        val moved = TrackRunSpec(
            name = audited.name,
            links = audited.links.mapIndexed { index, link ->
                if (index == 3) link.copy(position = link.position.add(0.01, 0.0, 0.0)) else link
            },
            joints = audited.joints,
            friction = audited.friction,
        )
        assertThrows(IllegalArgumentException::class.java) {
            B2AssemblyProbe.requireMatches(moved, compiled.side(side))
        }
    }

    // ---------------------------------------------------------------------
    // The record cannot omit a field
    // ---------------------------------------------------------------------

    private fun observation(
        side: TrackedSide = TrackedSide.LEFT,
        density: B2ChainDensity = B2ChainDensity.FULL,
        substeps: Int = B2Sampling.SETTLE_SUBSTEPS,
        worstPinGapM: Double = 1.0e-6,
        worstJointAngleViolationRad: Double = 0.0,
        linkWheelContactCount: Int = 6,
        wheelCount: Int = 8,
        worstLinkWheelPenetrationM: Double = 0.0,
        linkTerrainContactCount: Int = 5,
        linkLinkContactCount: Int = 0,
        longitudinalDriftM: Double = 0.01,
        suspensionTravelM: List<Double> = List(6) { 0.05 },
        suspensionForceN: List<Double> = List(6) { 20000.0 },
        chassisHeaveM: Double = -0.05,
        chassisPitchRad: Double = 0.0,
    ) = B2RunObservation(
        side = side,
        density = density,
        substeps = substeps,
        worstPinGapM = worstPinGapM,
        worstJointAngleViolationRad = worstJointAngleViolationRad,
        linkWheelContactCount = linkWheelContactCount,
        wheelCount = wheelCount,
        worstLinkWheelPenetrationM = worstLinkWheelPenetrationM,
        linkTerrainContactCount = linkTerrainContactCount,
        linkLinkContactCount = linkLinkContactCount,
        longitudinalDriftM = longitudinalDriftM,
        suspensionTravelM = suspensionTravelM,
        suspensionForceN = suspensionForceN,
        chassisHeaveM = chassisHeaveM,
        chassisPitchRad = chassisPitchRad,
    )

    /** A complete, well-formed record passes the structural check. */
    @Test
    fun aCompleteRecordHasNoStructuralProblems() {
        val record = observation()
        assertEquals(emptyList<String>(), record.problems(expectedStations = 6))
        assertTrue(record.isFullDensity)
        assertEquals(6.0 / 8.0, record.linkWheelCoverage, 1e-12)
    }

    /**
     * Self-contact inside one run is never acceptable: it means the collision
     * filter regressed and the chain is fighting itself. It cannot be traded
     * against another metric.
     */
    @Test
    fun selfContactingLinksAreRejected() {
        val record = observation(linkLinkContactCount = 3)
        assertTrue(
            record.problems(6).any { "same-owner links touched each other" in it },
            "self contact must be reported: ${record.problems(6)}",
        )
    }

    /** A station list that does not match the wheels would silently mis-attribute load. */
    @Test
    fun mismatchedStationCountsAreRejected() {
        val record = observation(
            suspensionTravelM = List(5) { 0.05 },
            suspensionForceN = List(5) { 20000.0 },
        )
        val problems = record.problems(expectedStations = 6)
        assertTrue(problems.any { "suspensionTravelM has 5 entries" in it }, "$problems")
        assertTrue(problems.any { "suspensionForceN has 5 entries" in it }, "$problems")
    }

    /** A `NaN` from an unwritten sample must fail loudly instead of comparing quietly. */
    @Test
    fun nonFiniteMeasurementsAreRejected() {
        val record = observation(
            worstPinGapM = Double.NaN,
            longitudinalDriftM = Double.POSITIVE_INFINITY,
            chassisHeaveM = Double.NaN,
        )
        val problems = record.problems(6)
        assertTrue(problems.any { "worstPinGapM is not finite" in it }, "$problems")
        assertTrue(problems.any { "longitudinalDriftM is not finite" in it }, "$problems")
        assertTrue(problems.any { "chassisHeaveM is not finite" in it }, "$problems")
    }

    /** Contact counts are bounded by the wheels that exist; more is a bookkeeping bug. */
    @Test
    fun impossibleContactCountsAreRejected() {
        val record = observation(linkWheelContactCount = 9, wheelCount = 8)
        assertTrue(
            record.problems(6).any { "outside 0..8" in it },
            "a contact count above the wheel count must be rejected",
        )
    }

    /** Penetration is reported as a depth; a negative value is a sign error, not a bonus. */
    @Test
    fun signErrorsInDerivedMetricsAreRejected() {
        val record = observation(
            worstLinkWheelPenetrationM = -0.01,
            longitudinalDriftM = -0.02,
            worstJointAngleViolationRad = -0.001,
        )
        val problems = record.problems(6)
        assertTrue(problems.any { "penetration is reported as a non-negative depth" in it }, "$problems")
        assertTrue(problems.any { "drift is reported as a magnitude" in it }, "$problems")
        assertTrue(problems.any { "violation cannot be negative" in it }, "$problems")
    }

    // ---------------------------------------------------------------------
    // Density claims
    // ---------------------------------------------------------------------

    /** A full-density claim must actually carry the audited link count. */
    @Test
    fun fullDensityRequiresTheAuditedLinkCount() {
        assertEquals("full density, 79 links", B2ChainDensity.FULL.describe(79, 79))
        val error = assertThrows(IllegalArgumentException::class.java) {
            B2ChainDensity.FULL.describe(40, 79)
        }
        assertTrue("requires 79 links" in error.message.orEmpty(), error.message.orEmpty())
    }

    /** A diagnostic chain may be described, but its label says it is not evidence. */
    @Test
    fun diagnosticDensityIsLabelledAsNotEvidence() {
        val label = B2ChainDensity.DIAGNOSTIC.describe(40, 79)
        assertTrue("DIAGNOSTIC ONLY" in label, label)
        assertTrue("not acceptance evidence" in label, label)
        assertFalse(observation(density = B2ChainDensity.DIAGNOSTIC).isFullDensity)
    }

    // ---------------------------------------------------------------------
    // What this contract refuses to invent
    // ---------------------------------------------------------------------

    /**
     * The core publishes no joint impulse, and Bullet's 6-Dof `appliedImpulse`
     * reads back 0. A "joint load" column here would be a comfortable zero, so
     * the record deliberately has none: load evidence comes from the suspension
     * row and from contact impulses.
     */
    @Test
    fun theRecordCarriesNoFabricatedJointLoad() {
        val fields = B2RunObservation::class.java.declaredFields.map { it.name.lowercase() }
        listOf("impulse", "jointload", "jointforce", "reaction").forEach { forbidden ->
            assertFalse(
                fields.any { forbidden in it },
                "B2RunObservation must not carry a '$forbidden' field: $fields",
            )
        }
        assertTrue(fields.any { "suspensionforcen" in it }, "row force is the load proxy: $fields")
    }

    /** The sampling windows are fixed here so two fixtures cannot compare unequal windows. */
    @Test
    fun theSamplingWindowsAreFixed() {
        assertEquals(1.0 / 60.0, B2Sampling.SUBSTEP_SECONDS, 1e-12)
        assertEquals(300, B2Sampling.SHORT_CHAIN_SUBSTEPS)
        assertEquals(600, B2Sampling.SETTLE_SUBSTEPS)
        assertTrue(
            B2Sampling.SETTLE_SUBSTEPS > B2Sampling.SHORT_CHAIN_SUBSTEPS,
            "the full-density window must be the longer one",
        )
    }

    /** The observation must survive reporting without a verdict attached. */
    @Test
    fun theDiagnosticsLineCarriesNoVerdict() {
        val text = observation().describe()
        listOf("pass", "fail", "ok").forEach { word ->
            assertFalse(
                text.lowercase().contains(word),
                "diagnostics must not imply a verdict: $text",
            )
        }
        assertTrue(text.contains("wheelContacts=6/8"), text)
        assertTrue(text.contains("drift="), text)
    }

    /**
     * Build a valid open chain from the audited shoe mass, shape and hinge
     * offsets.
     *
     * This exists only so the probe can be shown rejecting a non-audited chain.
     * It reuses the audited link mass, box extents and pivot offsets rather than
     * inventing them, so the only difference is the link count.
     */
    private fun shortOpenChain(audited: TrackRunSpec, linkCount: Int): TrackRunSpec {
        val template = audited.links.first()
        val hinge = audited.joints.first()
        val links = audited.links.take(linkCount)
        val joints = (0 until linkCount - 1).map { index ->
            hinge.copy(previousIndex = index, nextIndex = index + 1)
        }
        return TrackRunSpec(
            name = "${audited.name}_diagnostic",
            links = links,
            joints = joints,
            friction = audited.friction,
        ).also { probe ->
            require(template.massKg > 0f) { "audited shoe must have mass" }
            require(probe.links.size == linkCount) { "diagnostic chain lost links" }
        }
    }
}
