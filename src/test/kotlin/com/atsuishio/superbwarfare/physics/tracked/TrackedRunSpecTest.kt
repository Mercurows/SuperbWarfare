package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot

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
            val run = TrackedRunSpecs.compile(spec, side)
            assertTrue(run.problems().isEmpty(), "$side run spec problems: ${run.problems()}")
            assertEquals(spec.side(side).loop.linkCount, run.links.size, "$side physical links")
            assertEquals(run.links.size, run.joints.size, "$side hinges")
            assertEquals(
                spec.linkMassKg.toFloat(),
                run.links.first().massKg,
                "link mass must come from Track.LinkMass",
            )
        }
    }

    /**
     * Adjacent link *centres* are not one pitch apart: they are the midpoints of
     * neighbouring chords, so on a curved section they sit closer together. The
     * real invariant is that both hinge pins of a joint land on the same world
     * point, which is what the runtime hands to `New6Dof`.
     */
    @Test
    fun everyHingePinPairCoincides() {
        val spec = spec()
        val pitch = spec.pitch
        for (side in TrackedSide.entries) {
            val run = TrackedRunSpecs.compile(spec, side)
            run.joints.forEachIndexed { index, joint ->
                val previous = run.links[joint.previousIndex]
                val next = run.links[joint.nextIndex]
                val previousPin = hingePoint(previous, joint.pivotInPrevious)
                val nextPin = hingePoint(next, joint.pivotInNext)
                val gap = hypot(
                    hypot(previousPin.x - nextPin.x, previousPin.y - nextPin.y),
                    previousPin.z - nextPin.z,
                )
                assertTrue(gap < 1.0e-6, "$side joint $index pin gap $gap m")
                // The pin is half a pitch from its own link centre along local +Z.
                assertEquals(pitch * 0.5, joint.pivotInPrevious.z, 1.0e-9)
                assertEquals(-pitch * 0.5, joint.pivotInNext.z, 1.0e-9)
            }
        }
    }

    /** The adapter must emit exactly the placements the geometry audit measured. */
    @Test
    fun theRunSpecReusesTheAuditedPlacements() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            val compiled = spec.side(side)
            val run = TrackedRunSpecs.compile(spec, side)
            assertEquals(compiled.placements.size, run.links.size)
            compiled.placements.zip(run.links).forEach { (placement, link) ->
                assertEquals(placement.center.x, link.position.x, 1e-9, "link ${placement.index} x")
                assertEquals(placement.center.y, link.position.y, 1e-9, "link ${placement.index} y")
                assertEquals(placement.center.z, link.position.z, 1e-9, "link ${placement.index} z")
                assertEquals(placement.rotation.x.toDouble(), link.rotationX.toDouble(), 1e-6)
                assertEquals(placement.rotation.w.toDouble(), link.rotationW.toDouble(), 1e-6)
                assertEquals(compiled.linkTemplate.physical.halfExtents, (link.shape as cn.solarmoon.spark_core.physics.track.TrackRunLinkShape.Box).halfExtents)
            }
        }
    }

    /**
     * The core limit is symmetric, `[-halfAngleRad, +halfAngleRad]`, so the
     * half angle itself must cover the wrap. Doubling it in the assertion would
     * hide a hinge that is twice too narrow.
     */
    @Test
    fun theHingeLimitCoversTheAuthoredWrap() {
        val spec = spec()
        for (side in TrackedSide.entries) {
            assertTrue(TrackedRunSpecs.wrapFits(spec, side), "$side wrap exceeds the authored limit")
            val run = TrackedRunSpecs.compile(spec, side)
            val halfAngle = run.joints.first().halfAngleRad
            val wrap = Math.toRadians(TrackedGeometryAudit.maxJointAngleDegrees(spec.side(side).placements))
            assertTrue(
                halfAngle >= wrap,
                "$side hinge half angle $halfAngle rad does not cover the wrap $wrap rad",
            )
            assertEquals(
                Math.toRadians(spec.angularLimitDegrees).toFloat(),
                halfAngle,
                1e-7f,
                "$side hinge limit must be the authored limit",
            )
        }
    }

    @Test
    fun theRunSpecCarriesNoVehicleOrRenderConcepts() {
        val run: TrackRunSpec = TrackedRunSpecs.compile(spec(), TrackedSide.LEFT)
        val names = TrackRunSpec::class.java.methods.map { it.name.lowercase() }
        listOf("vehicle", "bone", "render", "skeleton", "model").forEach { forbidden ->
            assertFalse(names.any { forbidden in it }, "core TrackRunSpec exposes '$forbidden'")
        }
        assertEquals("superbwarfare:ztz_99a_track_left", run.name)
    }

    /** World-space pin position of one link end, using the link's authored pose. */
    private fun hingePoint(link: cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec, local: net.minecraft.world.phys.Vec3): net.minecraft.world.phys.Vec3 {
        val rotation = link.rotation
        val rotated = rotation.transform(org.joml.Vector3f(local.x.toFloat(), local.y.toFloat(), local.z.toFloat()))
        return net.minecraft.world.phys.Vec3(
            link.position.x + rotated.x,
            link.position.y + rotated.y,
            link.position.z + rotated.z,
        )
    }
}
