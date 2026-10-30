package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * S1 check that the audited mechanism can be expressed as a generic core
 * `TrackRunSpec` without inventing geometry.
 *
 * This stays a pure data test: it never creates a Bullet object. Assembly,
 * wheel contact and drive belong to B1/B2/S2.
 */
class TrackedRunSpecTest {
    private val resources = File("src/main/resources")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun spec(): TrackedVehicleSpec {
        val definition = json.decodeFromString(
            TrackedVehicleDefinition.serializer(),
            File(resources, "data/superbwarfare/sbw/tracked/ztz_99a.json").readText(),
        )
        val model = File(resources, "assets/superbwarfare/models/bedrock/vehicle/ztz_99a.geo.json").readText()
        return TrackedVehicleCompiler.compile("superbwarfare:ztz_99a", definition, model).requireSpec()
    }

    @Test
    fun bothSidesCompileToAClosedRunSpec() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            assertEquals(null, spec.side(side).legacy, "$side idler is fixed; no shoe chain")
            val runs = TrackedRunSpecs.compileRuns(spec, side)
            assertEquals(spec.side(side).runs.size, runs.size, "$side physical runs")
            runs.forEachIndexed { index, run ->
                assertTrue(run.problems().isEmpty(), "$side run $index problems: ${run.problems()}")
                assertEquals(1, run.links.size, "$side run $index is one box")
                assertTrue(run.joints.isEmpty(), "$side run $index has no internal hinge")
                assertEquals(spec.side(side).runs[index].massKg.toFloat(), run.links.first().massKg)
            }
        }
    }

    /**
     * Adjacent link *centres* are not one pitch apart: they are the midpoints of
     * neighbouring chords, so on a curved section they sit closer together. The
     * real invariant is that both hinge pins of a joint land on the same world
     * point, which is what the runtime hands to `New6Dof`.
     */
    @Test
    fun everyRunEndAnchorsTheWheelCentre() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            for (run in compiled.runs) {
                assertEquals(0.0, run.first.anchorInWheel.length(), 1e-9)
                assertEquals(0.0, run.second.anchorInWheel.length(), 1e-9)
                assertTrue(run.first.joint.freeLinearAxes.isEmpty(), "$side run ${run.index} first end locks translation")
                assertEquals(
                    1,
                    run.second.joint.linear.count { it is CompiledAxisLimit.Locked },
                    "$side run ${run.index} second end locks only the normal",
                )
            }
        }
    }

    /** The adapter must emit exactly the run boxes the geometry audit measured. */
    @Test
    fun theRunSpecReusesTheAuditedPlacements() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val runs = TrackedRunSpecs.compileRuns(spec, side)
            assertEquals(compiled.runs.size, runs.size)
            compiled.runs.zip(runs).forEach { (placement, run) ->
                val link = run.links.single()
                assertEquals(placement.center.x, link.position.x, 1e-9, "run ${placement.index} x")
                assertEquals(placement.center.y, link.position.y, 1e-9, "run ${placement.index} y")
                assertEquals(placement.center.z, link.position.z, 1e-9, "run ${placement.index} z")
                assertEquals(placement.rotation.x.toDouble(), link.rotationX.toDouble(), 1e-6)
                assertEquals(placement.rotation.w.toDouble(), link.rotationW.toDouble(), 1e-6)
                assertEquals(placement.halfExtents, (link.shape as cn.solarmoon.spark_core.physics.track.TrackRunLinkShape.Box).halfExtents)
            }
        }
    }

    /**
     * The core limit is symmetric, `[-halfAngleRad, +halfAngleRad]`, so the
     * half angle itself must cover the wrap. Doubling it in the assertion would
     * hide a hinge that is twice too narrow.
     */
    @Test
    fun theRunSpecCarriesNoVehicleOrRenderConcepts() {
        val run: TrackRunSpec = TrackedRunSpecs.compileRuns(spec(), TrackedSide.LEFT).first()
        val names = TrackRunSpec::class.java.methods.map { it.name.lowercase() }
        listOf("vehicle", "bone", "render", "skeleton", "model").forEach { forbidden ->
            assertFalse(names.any { forbidden in it }, "core TrackRunSpec exposes '$forbidden'")
        }
        assertEquals("superbwarfare:ztz_99a_track_left_run_0", run.name)
    }
}
