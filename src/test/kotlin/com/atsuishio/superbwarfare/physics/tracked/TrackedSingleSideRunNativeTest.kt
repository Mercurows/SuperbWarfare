package com.atsuishio.superbwarfare.physics.tracked

import com.jme3.math.Vector3f
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * B2.2: the complete single-side mechanism — eight wheel bodies, eight runs.
 *
 * B2.1 proved the end-constraint semantics on the smallest mechanism. This stage
 * asks whether the **whole side** stays one mechanism: every adjacent member
 * pair, including the closing run from the last wheel back to the first, in one
 * closed loop of constraints, under gravity.
 *
 * A side with no chassis falls as a whole; that is expected. The measurement is
 * therefore **relative** to the wheel bodies, not absolute world displacement:
 * an absolute test would call uniform free fall "drift" and a loose constraint
 * "stable" as long as everything fell together.
 *
 * What this stage does **not** claim: it carries no chassis and touches no
 * terrain. Load transfer belongs to B2.3.
 */
class TrackedSingleSideRunNativeTest {

    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun spec(): TrackedVehicleSpec = TrackedVehicleCompiler.compile(
        "superbwarfare:ztz_99a",
        json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        ),
        File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText(),
    ).requireSpec()

    private fun fullSide(spec: TrackedVehicleSpec, side: TrackedSide) =
        TrackedMechanismFixture.build(
            spec,
            side,
            spec.side(side).members.size,
            gravity = Vector3f(0f, -9.81f, 0f),
            kinematicWheels = true,
        )

    @Test
    fun theCompleteSideAssemblesWithOneRunPerMemberPair() {
        val vehicle = spec()
        for (side in TrackedSide.entries) {
            fullSide(vehicle, side).use { mechanism ->
                val memberCount = mechanism.view.members.size
                assertEquals(memberCount, mechanism.view.runs.size, "$side run count")
                assertEquals(memberCount, mechanism.view.wheels.size, "$side wheel count")
                assertEquals(memberCount * 2, mechanism.joints.size, "$side external end count")
                assertEquals(1, mechanism.view.runs.count { it.closesLoop }, "$side closing run")

                val closing = mechanism.view.runs.single { it.closesLoop }
                assertEquals(mechanism.view.members.last().wheelIndex, closing.firstWheelIndex)
                assertEquals(mechanism.view.members.first().wheelIndex, closing.secondWheelIndex)
            }
        }
    }

    /**
     * Under gravity the complete side must keep every first-end anchor and every
     * locked axle aligned.
     */
    @Test
    fun theCompleteSideHoldsItsConstraintsUnderGravity() {
        val vehicle = spec()
        for (side in TrackedSide.entries) {
            fullSide(vehicle, side).use { mechanism ->
                val before = mechanism.firstEndAnchorGap()
                assertTrue(before < 1e-4, "$side assembly anchor gap $before m")

                mechanism.step(B2Sampling.SETTLE_SUBSTEPS)

                val after = mechanism.firstEndAnchorGap()
                assertTrue(
                    after < 1e-2,
                    "$side must hold its anchors over ${B2Sampling.SETTLE_SUBSTEPS} substeps under gravity, " +
                        "gap grew to $after m",
                )
                val axis = mechanism.firstEndAxisError()
                assertTrue(axis < 1e-2, "$side first-end locked axes must stay aligned, worst $axis rad")
                val second = mechanism.secondEndAnchorGap()
                assertTrue(second < 0.05, "$side second-end anchor separation $second m")
            }
        }
    }

    /**
     * No run may drift relative to the wheel it is pinned to.
     *
     * The whole side free-falls together, so absolute motion is not the signal.
     * The distance from a run's centre to its first wheel centre is fixed by the
     * revolute, and must stay fixed.
     */
    @Test
    fun theCompleteSideDoesNotDriftRelativeToItsWheels() {
        val vehicle = spec()
        fullSide(vehicle, TrackedSide.LEFT).use { mechanism ->
            val start = mechanism.view.runs.associateWith { run ->
                mechanism.runBody(run.index).getPhysicsLocation(null)
                    .distance(mechanism.wheelBody(run.firstWheelIndex).getPhysicsLocation(null))
            }
            mechanism.step(B2Sampling.SETTLE_SUBSTEPS)
            mechanism.view.runs.forEach { run ->
                val now = mechanism.runBody(run.index).getPhysicsLocation(null)
                    .distance(mechanism.wheelBody(run.firstWheelIndex).getPhysicsLocation(null))
                val moved = abs(now - start.getValue(run))
                assertTrue(
                    moved < 0.02,
                    "run ${run.index} changed its distance to wheel ${run.firstWheelIndex} by $moved m",
                )
            }
        }
    }

    @Test
    fun theCompleteSideHasNoRunWheelPenetration() {
        val vehicle = spec()
        for (side in TrackedSide.entries) {
            fullSide(vehicle, side).use { mechanism ->
                val audit = TrackedRunGeometryAudit.audit(
                    mechanism.view.runs,
                    mechanism.view.wheels,
                    mechanism.view.belt,
                )
                assertEquals(emptyList<String>(), audit.problems(), "$side audit: ${audit.problems()}")
                assertTrue(
                    audit.minRunWheelGapM >= -TrackedRunGeometryAudit.CONTACT_TOLERANCE_M,
                    "$side deepest gap ${audit.minRunWheelGapM} m",
                )
            }
        }
    }

    /**
     * The closing run must actually close the loop.
     *
     * If it were missing, the last member would have a free end. Loading the last
     * wheel pulls on the closing run through its first-end revolute; the test
     * checks that the closing run still holds that anchor and that the rest of
     * the side remains constrained.
     */
    @Test
    fun theClosingRunCarriesTheLoop() {
        val vehicle = spec()
        fullSide(vehicle, TrackedSide.LEFT).use { mechanism ->
            val closing = mechanism.view.runs.single { it.closesLoop }
            val lastWheel = mechanism.wheelBody(closing.firstWheelIndex)
            val before = mechanism.runBody(closing.index).getPhysicsLocation(null)
                .distance(lastWheel.getPhysicsLocation(null))

            lastWheel.applyCentralForce(Vector3f(0f, 0f, 1200f))
            mechanism.step(180)

            val after = mechanism.runBody(closing.index).getPhysicsLocation(null)
                .distance(lastWheel.getPhysicsLocation(null))
            assertTrue(
                abs(after - before) < 0.02,
                "the closing run must stay pinned to the last wheel, distance changed " +
                    "${before} -> ${after} m",
            )
            assertTrue(
                mechanism.firstEndAnchorGap() < 2e-2,
                "the closing run must keep its anchor, gap ${mechanism.firstEndAnchorGap()} m",
            )
        }
    }
}
