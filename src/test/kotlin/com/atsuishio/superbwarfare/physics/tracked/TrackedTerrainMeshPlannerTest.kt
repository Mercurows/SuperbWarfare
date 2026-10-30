package com.atsuishio.superbwarfare.physics.tracked

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import com.jme3.math.Vector3f

/**
 * Pure geometry contract for the tracked terrain mesh.
 *
 * These cases deliberately use detached boxes. No Level and no Bullet object
 * is created: the planner must prove the ramp/wall decision before the runtime
 * is allowed to install anything.
 */
class TrackedTerrainMeshPlannerTest {

    private val terrain = CompiledTerrain(
        rampsEnabled = true,
        maxClimbM = 1.0,
        stepM = 1.0,
        windowRadiusBlocks = 8,
        verticalAboveBlocks = 6,
        verticalBelowBlocks = 8,
    )

    private fun box(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int) =
        TerrainBox(x0.toDouble(), y0.toDouble(), z0.toDouble(), x1.toDouble(), y1.toDouble(), z1.toDouble())

    private fun floorAndStep(highBlocks: Int = 1, stepBlocks: Int = 1): List<TerrainBox> {
        val boxes = ArrayList<TerrainBox>()
        // Low flat run: two one-metre cells.
        for (x in 0..2) boxes += box(x, 0, 0, x + 1, 1, 2)
        // Raised cell, its top proving a one-block (or taller) rise.
        for (x in 3..3 + stepBlocks - 1) boxes += box(x, 0, 0, x + 1, 1 + highBlocks, 2)
        return boxes
    }

    @Test
    fun oneBlockRiseProducesAFixedLengthRamp() {
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(),
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 1L,
        )
        val ramp = plan.ramps.single()
        assertEquals(RampDirection.EAST, ramp.direction)
        assertEquals(1.0, ramp.riseM, 1.0e-6)
        assertEquals(1.0, ramp.runM, 1.0e-6)
        assertEquals(2.0, ramp.lowX, 1.0e-6)
        assertEquals(3.0, ramp.highX, 1.0e-6)
        assertTrue(plan.triangleCount > 0)
        assertTrue(plan.wallQuadCount > 0, "the non-ramp vertical faces must remain walls")
    }

    @Test
    fun twoBlockWallIsNotTurnedIntoARamp() {
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(highBlocks = 2),
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 2L,
        )
        assertTrue(plan.ramps.isEmpty())
        assertTrue(plan.wallQuadCount > 0)
    }

    @Test
    fun consecutiveOneBlockStairsGetOneRampPerRise() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..2) boxes += box(x, 0, 0, x + 1, 1, 2)
        for (x in 3..5) boxes += box(x, 0, 0, x + 1, 1 + (x - 2), 2)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 3L,
        )
        assertEquals(3, plan.ramps.size)
        assertEquals(listOf(1.0, 1.0, 1.0), plan.ramps.map { it.riseM })
        assertEquals(listOf(2.0, 3.0, 4.0), plan.ramps.map { it.lowX })
        assertEquals(listOf(3.0, 4.0, 5.0), plan.ramps.map { it.highX })
    }

    @Test
    fun aShortApproachUsesTheCoveredLengthInsteadOfSpanningAir() {
        val terrain = terrain.copy(stepM = 3.0)
        // Only one metre of low surface exists. The authored 3 m step must not
        // be required, and the triangle must not be extended into air.
        val shortRun = listOf(
            box(2, 0, 0, 3, 1, 2),
            box(3, 0, 0, 4, 2, 2),
        )
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = shortRun,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 4L,
        )
        val ramp = plan.ramps.single()
        assertEquals(1.0, ramp.runM, 1.0e-6)
        assertEquals(2.0, ramp.lowX, 1.0e-6)
        assertEquals(3.0, ramp.highX, 1.0e-6)
        assertTrue(plan.wallQuadCount > 0)
    }

    @Test
    fun aThreeMetreStepStillRampsEveryOneBlockStair() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..5) boxes += box(x, 0, 0, x + 1, 1 + x, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain.copy(stepM = 3.0),
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 41L,
        )
        assertEquals(5, plan.ramps.size, "each one-block tread needs its own ramp, not a red wall: ${plan.ramps}")
        assertTrue(plan.ramps.all { it.runM == 1.0 && it.riseM == 1.0 })
    }

    @Test
    fun disabledRampsStillProduceATerrainMeshWithVerticalRises() {
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(),
            terrain = terrain.copy(rampsEnabled = false),
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 5L,
        )
        assertTrue(plan.ramps.isEmpty())
        assertTrue(plan.triangleCount > 0)
        assertTrue(plan.wallQuadCount > 0)
    }

    @Test
    fun aFlatFloorHasNoRamps() {
        val boxes = ArrayList<TerrainBox>()
        for (x in -6..6) for (z in -6..6) boxes += box(x, 0, z, x + 1, 1, z + 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = -6.0,
            originY = 0.0,
            originZ = -6.0,
            contentHash = 7L,
        )
        assertTrue(plan.ramps.isEmpty(), "a flat floor must never generate ramps: ${plan.ramps}")
        assertTrue(plan.triangleCount > 0)
    }

    @Test
    fun aPitDoesNotInventAGroundSurface() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..2) boxes += box(x, 0, 0, x + 1, 1, 2)
        // No high cell and no fill: this is known air, not a guessed floor.
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 6L,
        )
        assertTrue(plan.ramps.isEmpty())
        assertTrue(plan.triangleCount > 0)
    }

    private fun hasVerticalQuadAtX(plan: TerrainMeshPlan, x: Double, normalX: Double): Boolean {
        val localX = (x - plan.originX).toFloat()
        for (base in plan.indices.indices step 6) {
            val points = (0..3).map { vertex ->
                val index = when (vertex) {
                    0 -> plan.indices[base]
                    1 -> plan.indices[base + 1]
                    2 -> plan.indices[base + 2]
                    else -> plan.indices[base + 5]
                }
                Vector3f(
                    plan.vertices[index * 3],
                    plan.vertices[index * 3 + 1],
                    plan.vertices[index * 3 + 2],
                )
            }
            if (points.all { kotlin.math.abs(it.x - localX) < 1.0e-4f } &&
                points.map { it.z }.distinct().size > 1 &&
                points.map { it.y }.distinct().size > 1
            ) {
                val normal = points[1].subtract(points[0]).cross(points[2].subtract(points[0]))
                if (normal.x * normalX > 0f) return true
            }
        }
        return false
    }

    @Test
    fun sameFacingCoplanarWallsDoNotOccludeEachOther() {
        val wall = box(0, 0, 0, 1, 2, 1)
        val duplicate = wall.copy()
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = listOf(wall, duplicate), terrain = terrain,
            originX = 0.0, originY = 0.0, originZ = 0.0, contentHash = 8L,
        )
        assertTrue(hasVerticalQuadAtX(plan, 0.0, -1.0), "the outward-facing MIN-X wall must remain")
        assertTrue(hasVerticalQuadAtX(plan, 1.0, 1.0), "the outward-facing MAX-X wall must remain")
    }

    @Test
    fun oppositeFacingInternalWallsAreRemoved() {
        val left = box(0, 0, 0, 1, 2, 1)
        val right = box(1, 0, 0, 2, 2, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = listOf(left, right), terrain = terrain,
            originX = 0.0, originY = 0.0, originZ = 0.0, contentHash = 9L,
        )
        assertFalse(hasVerticalQuadAtX(plan, 1.0, 1.0), "left MAX-X internal wall must be removed")
        assertFalse(hasVerticalQuadAtX(plan, 1.0, -1.0), "right MIN-X internal wall must be removed")
        assertTrue(hasVerticalQuadAtX(plan, 0.0, -1.0), "the exposed outside wall must remain")
    }

    @Test
    fun generatedRampTrianglesAreMarkedAndInclined() {
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(),
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 14L,
        )
        assertEquals(plan.indices.size / 3, plan.rampTriangles.size)
        assertTrue(plan.rampTriangles.any { it }, "the generated ramp must mark its own triangles")
        assertTrue(plan.rampTriangles.any { !it }, "block faces must stay unmarked")
        var inclined = false
        for (triangle in plan.rampTriangles.indices) {
            if (!plan.rampTriangles[triangle]) continue
            val ys = (0..2).map { vertex ->
                val index = plan.indices[triangle * 3 + vertex]
                plan.vertices[index * 3 + 1]
            }
            if (ys.max() - ys.min() > 0.2f) inclined = true
        }
        assertTrue(inclined, "a ramp triangle must use the real slope, not a flat stand-in")
        val mesh = TrackedTerrainDebugMesh.from("superbwarfare:ztz_99a", plan)
        assertFalse(mesh.vertices === plan.vertices, "the debug copy must not share the planner buffer")
        val selected = TrackedTerrainDebugSelection.of(mesh, 1.5, 1.0, 1.0)
        assertTrue(selected.triangleCount > 0)
        assertFalse(selected.truncated)
        assertTrue(
            (9 until selected.packed.size step TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE)
                .any { selected.packed[it] > 0.5f },
            "the debug selection must keep the ramp flag from the installed mesh",
        )
    }

    @Test
    fun aSmallYawDoesNotChangeTheRampHeadingBucket() {
        val straight = RampGate.forVehicle(0.0, 0.0, 0.0, 1.0, 2.0)
        val nudged = RampGate.forVehicle(0.0, 0.0, 0.05, 1.0, 2.0)
        assertEquals(straight.headingBucket, nudged.headingBucket)
        assertEquals(0.0, straight.forwardX, 1.0e-6)
        assertEquals(1.0, straight.forwardZ, 1.0e-6)
    }

    private fun forwardGate(x: Double, z: Double, lateral: Double = 1.25) = RampGate(
        originX = x,
        originZ = z,
        forwardX = 0.0,
        forwardZ = 1.0,
        lateralHalfWidthM = lateral,
    )

    @Test
    fun aSideRiseIsNotARampWhenTheNoseFacesForward() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..4) for (z in 0..4) boxes += box(x, 0, z, x + 1, 1, z + 1)
        for (x in 1..3) boxes += box(x, 0, 4, x + 1, 2, 5)
        for (z in 1..3) boxes += box(5, 0, z, 6, 2, z + 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 10L,
            rampGate = forwardGate(2.5, 2.5),
        )
        assertTrue(plan.ramps.isNotEmpty(), "the step in front of the nose must still ramp")
        assertTrue(plan.ramps.all { it.direction == RampDirection.SOUTH }, "side rises must stay walls: ${plan.ramps}")
        assertTrue(plan.wallQuadCount > 0, "the rejected side rise must remain a wall")
    }

    @Test
    fun aRiseBehindTheHullRampsWhenItClimbsAlongTheTail() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 1..3) for (z in 0..5) boxes += box(x, 0, z, x + 1, 1, z + 1)
        for (x in 1..3) boxes += box(x, 0, 0, x + 1, 2, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 11L,
            rampGate = forwardGate(2.5, 4.5),
        )
        assertTrue(plan.ramps.isNotEmpty(), "a rise behind the hull must ramp: ${plan.ramps}")
        assertTrue(plan.ramps.all { it.direction == RampDirection.NORTH }, "the rear rise climbs toward the tail: ${plan.ramps}")
    }

    @Test
    fun noseAndTailRampsStayInsideTheTrackCorridor() {
        val gate = RampGate.forVehicle(0.0, 0.0, 0.0, 1.0, 2.0)
        assertTrue(gate.allows(RampDirection.SOUTH, 4.0, 0), "a rise ahead of the nose ramps")
        assertTrue(gate.allows(RampDirection.NORTH, -4.0, 0), "a rise behind the tail ramps")
        assertFalse(gate.allows(RampDirection.EAST, 4.0, 0), "a side climb stays a wall")
        assertFalse(gate.allows(RampDirection.SOUTH, 4.0, 5), "a longitudinal climb beside the tracks stays a wall")
    }

    @Test
    fun aPartialCellDoesNotInventARampOverAir() {
        val boxes = listOf(
            TerrainBox(2.0, 0.0, 0.0, 2.4, 1.0, 1.0),
            box(3, 0, 0, 4, 2, 1),
        )
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 12L,
        )
        assertTrue(plan.ramps.isEmpty(), "a stair that covers part of the approach must not span the rest: ${plan.ramps}")
        assertTrue(plan.wallQuadCount > 0, "the rise stays a wall when the approach is not covered")
    }

    @Test
    fun aLongRampKeepsVerticesUnderTheMiddleWheels() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..2) boxes += box(x, 0, 0, x + 1, 1, 1)
        boxes += box(3, 0, 0, 4, 2, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain.copy(stepM = 3.0),
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 13L,
        )
        val ramp = plan.ramps.single()
        assertEquals(RampDirection.EAST, ramp.direction)
        assertEquals(0.0, ramp.lowX, 1.0e-6)
        assertEquals(3.0, ramp.highX, 1.0e-6)
        var middle = false
        var belowFloor = false
        var downward = false
        for (cursor in plan.indices.indices step 3) {
            val points = (0..2).map { vertex ->
                val index = plan.indices[cursor + vertex]
                Vector3f(
                    plan.vertices[index * 3],
                    plan.vertices[index * 3 + 1],
                    plan.vertices[index * 3 + 2],
                )
            }
            val normal = points[1].subtract(points[0]).cross(points[2].subtract(points[0]))
            val sloped = points.any { it.y > 1.05f && it.y < 1.95f }
            if (!sloped) continue
            if (normal.y <= 0f) downward = true
            if (points.any { it.y < 0.99f }) belowFloor = true
            if (points.any { it.x in 0.9f..1.1f && it.y in 1.2f..1.5f }) middle = true
        }
        assertTrue(middle, "a 3 m ramp needs a vertex under the middle of the slope")
        assertFalse(belowFloor, "the ramp must not drop below the approach surface")
        assertFalse(downward, "the ramp normal must point up")
    }

    @Test
    fun aRiseUnderTheHullRampsAlongTheVehicleAxis() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 1..3) for (z in 1..4) boxes += box(x, 0, z, x + 1, 1, z + 1)
        for (x in 1..3) boxes += box(x, 0, 2, x + 1, 2, 3)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 15L,
            rampGate = forwardGate(2.5, 2.5),
        )
        assertTrue(plan.ramps.any { it.direction == RampDirection.SOUTH }, "a rise under the hull must ramp: ${plan.ramps}")
        assertTrue(
            plan.ramps.all { it.direction == RampDirection.SOUTH || it.direction == RampDirection.NORTH },
            "climbs under the hull stay on the vehicle axis: ${plan.ramps}",
        )
    }

    @Test
    fun crossingRampsKeepTheLowerSurfaceInsteadOfPenetrating() {
        val boxes = ArrayList<TerrainBox>()
        for (x in listOf(0, 2, 3, 5, 6)) boxes += box(x, 0, 0, x + 1, 1, 1)
        boxes += box(1, 0, 0, 2, 2, 1)
        boxes += box(4, 0, 0, 5, 2, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain.copy(stepM = 3.0),
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 16L,
        )
        val east = plan.ramps.filter { it.direction == RampDirection.EAST && it.highX in 3.5..4.5 }
        val west = plan.ramps.filter { it.direction == RampDirection.WEST && it.highX in 1.5..2.5 }
        assertTrue(east.isNotEmpty() && west.isNotEmpty(), "both climbs must remain: ${plan.ramps}")
        var eastOwned = false
        var westOwned = false
        var x = 2.125
        while (x < 3.9) {
            val eastHeight = east.minOf { rampHeight(it, x) }
            val westHeight = west.minOf { rampHeight(it, x) }
            val lower = minOf(eastHeight, westHeight)
            val heights = rampTriangleHeights(plan, x, 0.5)
            assertTrue(heights.isNotEmpty(), "the overlap at x=$x must still have a surface")
            assertTrue(
                heights.all { it <= lower + 0.08 },
                "a crossing ramp must not stay above the lower surface at x=$x: $heights lower=$lower",
            )
            if (heights.any { kotlin.math.abs(it - eastHeight) < 0.08 }) eastOwned = true
            if (heights.any { kotlin.math.abs(it - westHeight) < 0.08 }) westOwned = true
            x += 0.25
        }
        assertTrue(eastOwned && westOwned, "each climb must keep the side where it is the lower surface")
    }

    @Test
    fun aDeckRampStaysWhenItIsAnotherLayer() {
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..3) boxes += box(x, 0, 0, x + 1, 1, 1)
        boxes += box(3, 0, 0, 4, 2, 1)
        for (x in 0..3) boxes += box(x, 5, 0, x + 1, 6, 1)
        boxes += box(3, 5, 0, 4, 7, 1)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 17L,
        )
        assertTrue(plan.ramps.any { it.lowY < 2.0 }, "the ground ramp must remain: ${plan.ramps}")
        assertTrue(plan.ramps.any { it.lowY > 5.0 }, "the deck ramp is another layer and must remain: ${plan.ramps}")
    }

    @Test
    fun aRampThroughTheGearIsRefusedButAWheelRestingOnItIsNot() {
        val cutting = GearProbe(2.5, 1.5, 1.0, 0.35, 0.3, 0.35)
        val cut = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(),
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 18L,
            gear = listOf(cutting),
        )
        assertTrue(cut.ramps.isEmpty(), "a ramp through the gear centre must stay a wall: ${cut.ramps}")
        assertTrue(cut.wallQuadCount > 0, "refusing the ramp must leave the riser")

        val resting = GearProbe(2.15, 1.35, 1.0, 0.35, 0.3, 0.35)
        val kept = TrackedTerrainMeshPlanner.build(
            boxes = floorAndStep(),
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 19L,
            gear = listOf(resting),
        )
        assertEquals(1, kept.ramps.size, "a wheel sitting on the foot must not delete the ramp")
    }

    private fun rampHeight(ramp: TerrainRampPlan, x: Double): Double {
        val t = when (ramp.direction) {
            RampDirection.EAST -> (x - ramp.lowX) / ramp.runM
            RampDirection.WEST -> (ramp.lowX - x) / ramp.runM
            else -> 0.0
        }.coerceIn(0.0, 1.0)
        return ramp.lowY + (ramp.highY - ramp.lowY) * t
    }

    private fun rampTriangleHeights(plan: TerrainMeshPlan, x: Double, z: Double): List<Double> {
        val out = ArrayList<Double>()
        for (triangle in plan.rampTriangles.indices) {
            if (!plan.rampTriangles[triangle]) continue
            val points = (0..2).map { vertex ->
                val index = plan.indices[triangle * 3 + vertex]
                Vector3f(
                    (plan.originX + plan.vertices[index * 3]).toFloat(),
                    (plan.originY + plan.vertices[index * 3 + 1]).toFloat(),
                    (plan.originZ + plan.vertices[index * 3 + 2]).toFloat(),
                )
            }
            triangleHeightAt(points, x, z)?.let { out += it }
        }
        return out
    }

    private fun triangleHeightAt(points: List<Vector3f>, x: Double, z: Double): Double? {
        val a = points[0]
        val b = points[1]
        val c = points[2]
        val denom = (b.z - c.z) * (a.x - c.x) + (c.x - b.x) * (a.z - c.z)
        if (kotlin.math.abs(denom) < 1.0e-6f) return null
        val u = ((b.z - c.z) * (x - c.x) + (c.x - b.x) * (z - c.z)) / denom
        val v = ((c.z - a.z) * (x - c.x) + (a.x - c.x) * (z - c.z)) / denom
        val w = 1.0 - u - v
        if (u < -1.0e-3 || v < -1.0e-3 || w < -1.0e-3) return null
        return u * a.y + v * b.y + w * c.y
    }
}
