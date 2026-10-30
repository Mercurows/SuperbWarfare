package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import cn.solarmoon.spark_core.physics.body.PhysicsJoints
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.CylinderCollisionShape
import com.jme3.bullet.collision.shapes.MeshCollisionShape
import com.jme3.bullet.collision.shapes.infos.IndexedMesh
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Native proof that the generated terrain geometry is the collision surface.
 *
 * The ray heights are authored from the same boxes the planner consumes:
 * low platform at y=1, ramp top at y=1.5 halfway through the run, high platform
 * at y=2. No raw block AABB body exists in this space.
 */
class TrackedTerrainMeshNativeTest {

    private fun box(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int) =
        TerrainBox(x0.toDouble(), y0.toDouble(), z0.toDouble(), x1.toDouble(), y1.toDouble(), z1.toDouble())

    private fun plan(): TerrainMeshPlan {
        val terrain = CompiledTerrain(
            rampsEnabled = true,
            maxClimbM = 1.0,
            stepM = 1.0,
            windowRadiusBlocks = 8,
            verticalAboveBlocks = 6,
            verticalBelowBlocks = 8,
        )
        val boxes = ArrayList<TerrainBox>()
        for (x in 0..2) boxes += box(x, 0, 0, x + 1, 1, 2)
        boxes += box(3, 0, 0, 4, 2, 2)
        return TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 1L,
        )
    }

    @Test
    fun theMeshRampIsTheTerrainSurface() {        NativePhysicsLoader.load()
        val plan = plan()
        val points = Array(plan.vertices.size / 3) { i ->
            Vector3f(plan.vertices[i * 3], plan.vertices[i * 3 + 1], plan.vertices[i * 3 + 2])
        }
        val shape = MeshCollisionShape(true, IndexedMesh(points, plan.indices)).apply { margin = .001f }
        val body = PhysicsRigidBody(shape, 0f)
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        try {
            space.add(body)
            fun down(x: Float): Float {
                val hits = space.rayTest(Vector3f(x, 4f, 1f), Vector3f(x, -1f, 1f))
                assertTrue(hits.isNotEmpty(), "terrain mesh must be hit at x=$x")
                return hits.first().hitFraction.let { 4f + (-1f - 4f) * it }
            }
            assertEquals(1f, down(1.5f), .02f, "low platform")
            assertEquals(1.5f, down(2.5f), .03f, "the ramp top is a plane from y=1 to y=2")
            assertEquals(2f, down(3.5f), .02f, "high platform")
        } finally {
            space.destroy()
        }
    }

    @Test
    fun aRoadWheelCylinderRestsOnTheRampRatherThanTheFloor() {
        NativePhysicsLoader.load()
        val terrain = CompiledTerrain(true, 1.0, 3.0, 8, 6, 8)
        val boxes = ArrayList<TerrainBox>()
        for (z in 0..2) boxes += box(0, 0, z, 2, 1, z + 1)
        boxes += box(0, 0, 3, 2, 2, 4)
        val plan = TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            contentHash = 2L,
        )
        val ramp = plan.ramps.single()
        assertEquals(RampDirection.SOUTH, ramp.direction)
        assertEquals(3.0, ramp.runM, 1.0e-6)
        val radius = 0.53125f
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        try {
            space.setGravity(Vector3f(0f, -9.81f, 0f))
            val points = Array(plan.vertices.size / 3) { i ->
                Vector3f(plan.vertices[i * 3], plan.vertices[i * 3 + 1], plan.vertices[i * 3 + 2])
            }
            val ground = PhysicsRigidBody(
                MeshCollisionShape(true, IndexedMesh(points, plan.indices)).apply { margin = .001f },
                0f,
            )
            ground.friction = 1.2f
            space.add(ground)
            val wheel = PhysicsRigidBody(
                CylinderCollisionShape(Vector3f(0.2f, radius, radius), PhysicsSpace.AXIS_X).apply { margin = .001f },
                900f,
            )
            wheel.friction = 1.2f
            wheel.setDamping(0.2f, 0.2f)
            wheel.setSleepingThresholds(0f, 0f)
            // Middle of the 3 m ramp: surface y = 1.5. A miss falls to the floor at y = 1.
            val contactX = 1f
            val contactZ = 1.5f
            wheel.setPhysicsLocation(Vector3f(contactX, 1.5f + radius + 0.05f, contactZ))
            space.add(wheel)
            // A free cylinder rolls off a slope even when the slope is solid.
            // The slider removes that motion so the measured height is the contact.
            val rail = PhysicsRigidBody(BoxCollisionShape(0.05f), 0f)
            rail.setPhysicsLocation(Vector3f(contactX, 6f, contactZ))
            space.add(rail)
            val joint = New6Dof(
                rail, wheel,
                Vector3f(0f, -6f + 1.5f + radius + 0.05f, 0f),
                Vector3f(),
                Matrix3f(), Matrix3f(),
                RotationOrder.XYZ,
            )
            PhysicsJoints.lock(joint, PhysicsJoints.LIN_X)
            PhysicsJoints.free(joint, PhysicsJoints.LIN_Y)
            PhysicsJoints.lock(joint, PhysicsJoints.LIN_Z)
            PhysicsJoints.lock(joint, PhysicsJoints.ANG_X)
            PhysicsJoints.lock(joint, PhysicsJoints.ANG_Y)
            PhysicsJoints.lock(joint, PhysicsJoints.ANG_Z)
            space.addJoint(joint)
            repeat(240) { space.update(1f / 60f, 0, false, true, false) }
            val at = wheel.getPhysicsLocation(null)
            assertEquals(1.5f + radius, at.y, 0.08f, "the road wheel must rest on the ramp, not the floor under it")
            assertEquals(contactZ, at.z, 0.02f, "the slider must keep the wheel over the ramp")
            assertTrue(abs(wheel.getLinearVelocity(null).y) < 0.15f, "it must settle")
        } finally {
            space.destroy()
        }
    }
}
