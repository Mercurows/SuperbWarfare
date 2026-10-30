package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.NativePhysicsLoader
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.collision.shapes.MeshCollisionShape
import com.jme3.bullet.collision.shapes.infos.IndexedMesh
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Does a mesh floor actually carry weight?
 *
 * The tracked belts are rigid boxes resting on generated terrain. A triangle
 * mesh is infinitely thin, so this isolates whether Bullet generates and keeps
 * a supporting manifold for a flat box on the mesh, without any vehicle code.
 */
class TrackedTerrainMeshSupportTest {

    private fun box(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int) =
        TerrainBox(x0.toDouble(), y0.toDouble(), z0.toDouble(), x1.toDouble(), y1.toDouble(), z1.toDouble())

    private fun plan(): TerrainMeshPlan {
        val terrain = CompiledTerrain(false, 1.0, 1.0, 8, 6, 8)
        val boxes = ArrayList<TerrainBox>()
        for (x in -6..6) for (z in -6..6) boxes += box(x, 0, z, x + 1, 1, z + 1)
        return TrackedTerrainMeshPlanner.build(
            boxes = boxes,
            terrain = terrain,
            originX = -6.0,
            originY = 0.0,
            originZ = -6.0,
            contentHash = 1L,
        )
    }

    @Test
    fun aBoxRestsOnTheFlatMesh() {
        NativePhysicsLoader.load()
        val plan = plan()
        val points = Array(plan.vertices.size / 3) { i ->
            Vector3f(plan.vertices[i * 3], plan.vertices[i * 3 + 1], plan.vertices[i * 3 + 2])
        }
        val space = PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT)
        try {
            space.setGravity(Vector3f(0f, -9.81f, 0f))
            val terrain = PhysicsRigidBody(MeshCollisionShape(true, IndexedMesh(points, plan.indices)).apply {
                margin = .001f
            }, 0f)
            terrain.setPhysicsLocation(Vector3f(-6f, 0f, -6f))
            space.add(terrain)
            val plate = PhysicsRigidBody(BoxCollisionShape(.4f, .1f, 1.5f).apply { margin = .001f }, 1000f)
            plate.friction = .8f
            // Start above the surface: a triangle mesh has no interior, so a
            // body spawned already inside it is pushed out of whichever face
            // the solver picks. Falling onto the mesh is the real case.
            plate.setPhysicsLocation(Vector3f(.5f, 2.5f, .5f))
            space.add(plate)
            val before = plate.getPhysicsLocation(null).y
            repeat(240) { space.update(1f / 60f, 0, false, true, false) }
            val after = plate.getPhysicsLocation(null).y
            assertEquals(1.1f, after, .03f, "a box must rest on the mesh top (was $before)")
            assertTrue(kotlin.math.abs(plate.getLinearVelocity(null).y) < .05f, "it must settle, not sink")
            assertTrue(plan.indices.isNotEmpty())
            // Each top and bottom is one quad (6 indices); the four outer rims
            // add a wall quad per boundary block. A flat slab must not collapse
            // into a handful of triangles.
            val quads = plan.indices.size / 6
            assertTrue(quads >= 169, "every supported block needs its own top quad, got $quads quads")
        } finally {
            space.destroy()
        }
    }
}
