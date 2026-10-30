package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import cn.solarmoon.spark_core.physics.body.collideWithOwnerGroups
import cn.solarmoon.spark_core.physics.body.name
import cn.solarmoon.spark_core.physics.body.owner
import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.terrain.TerrainRevisionToken
import com.jme3.bullet.collision.shapes.MeshCollisionShape
import com.jme3.bullet.collision.shapes.infos.IndexedMesh
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f

/**
 * Owns the single static Bullet mesh that represents terrain for one tracked
 * vehicle. The proxy is replaced atomically on the physics thread; it is never
 * accompanied by a raw AABB terrain body.
 */
enum class TerrainMeshInstallResult { REJECTED, REUSED, REPLACED }

class TrackedTerrainMeshProxyRuntime(
    private val level: PhysicsLevel,
    private val host: PhysicsHost,
    private val vehicleId: String,
) {
    @Volatile private var body: PhysicsRigidBody? = null
    @Volatile var installedHash: Long = Long.MIN_VALUE
        private set
    @Volatile var rampCount: Int = 0
        private set
    @Volatile var triangleCount: Int = 0
        private set
    @Volatile var wallQuadCount: Int = 0
        private set
    @Volatile var ready: Boolean = false
        private set
    /** World Y of the lowest and highest upward mesh faces. NaN until a mesh is installed. */
    @Volatile var installedFloorMinY: Double = Double.NaN
        private set
    @Volatile var installedFloorMaxY: Double = Double.NaN
        private set
    /**
     * Copy of the mesh last handed to [MeshCollisionShape]. Null when no mesh
     * is installed. The epoch changes on every install or close so a debug
     * viewer can send the new triangles without polling native state.
     */
    @Volatile var debugMesh: TrackedTerrainDebugMesh? = null
        private set
    @Volatile var debugEpoch: Long = 0
        private set

    /** Terrain-cache revision the installed mesh was built from. */
    @Volatile var installedRevision: Long = Long.MIN_VALUE
        private set

    /** Request id of the installed mesh. */
    @Volatile var installedRequestId: Long = -1L
        private set

    /**
     * Install [plan] on the physics thread. Returns true when the body/state
     * changed. An empty but complete plan removes the old body and still makes
     * the proxy ready: known air is not "unknown".
     *
     * The identity checks are the point of this method. A build runs on a worker
     * and the hull keeps driving meanwhile, so a plan can arrive after the terrain
     * it describes has already changed. Installing it would put the vehicle on
     * geometry that no longer exists, which is worse than leaving it waiting.
     */
    fun install(
        plan: TerrainMeshPlan,
        expectedRequestId: Long,
        expectedGeneration: Long,
        currentRevision: TerrainRevisionToken?,
        beforeInstall: () -> Boolean,
    ): TerrainMeshInstallResult {
        level.assertPhysicsThread()
        if (!plan.complete || plan.terrainRevision == null || plan.terrainRevision != currentRevision ||
            plan.requestId != expectedRequestId || plan.runtimeGeneration != expectedGeneration ||
            plan.requestId <= installedRequestId || !beforeInstall()) return TerrainMeshInstallResult.REJECTED
        if (ready && installedHash == plan.contentHash) {
            // Same geometry, newer request: adopt the identity so a later stale
            // plan cannot win the `installedRequestId` comparison.
            installedRequestId = plan.requestId
            installedRevision = plan.terrainRevisionHash
            return TerrainMeshInstallResult.REUSED
        }

        val replacement = if (plan.isEmpty) null else createBody(plan)
        val previous = body
        // Add before remove: the vehicle must never spend a substep with no
        // terrain collision at all while the swap is in progress.
        if (replacement != null) level.addBody(replacement)
        try {
            // Keep the previous owner filter installed until removal succeeds.
            // If removal fails, roll the candidate back and leave the old mesh
            // and its collision ownership intact.
            if (previous != null) {
                level.removeBody(previous)
                TrackedTerrainCollision.unregister(previous)
            }
        } catch (error: Throwable) {
            if (replacement != null) {
                TrackedTerrainCollision.unregister(replacement)
                try {
                    level.removeBody(replacement)
                } catch (cleanupError: Throwable) {
                    error.addSuppressed(cleanupError)
                }
            }
            if (previous != null) TrackedTerrainCollision.register(previous, host)
            throw error
        }

        body = replacement
        installedHash = plan.contentHash
        installedRevision = plan.terrainRevisionHash
        installedRequestId = plan.requestId
        rampCount = plan.ramps.size
        triangleCount = plan.triangleCount
        wallQuadCount = plan.wallQuadCount
        val floor = upwardFloorRange(plan)
        installedFloorMinY = floor?.first ?: Double.NaN
        installedFloorMaxY = floor?.second ?: Double.NaN
        debugMesh = if (plan.isEmpty) null else TrackedTerrainDebugMesh.from(vehicleId, plan)
        debugEpoch++
        ready = true
        return TerrainMeshInstallResult.REPLACED
    }

    fun invalidate() {
        level.assertPhysicsThread()
        ready = false
    }

    fun closeNow() {
        level.assertPhysicsThread()
        val old = body ?: run {
            ready = false
            installedHash = Long.MIN_VALUE
            installedRevision = Long.MIN_VALUE
            installedRequestId = -1L
            installedFloorMinY = Double.NaN
            installedFloorMaxY = Double.NaN
            debugMesh = null
            debugEpoch++
            return
        }
        level.removeBody(old)
        TrackedTerrainCollision.unregister(old)
        body = null
        ready = false
        installedHash = Long.MIN_VALUE
        installedRevision = Long.MIN_VALUE
        installedRequestId = -1L
        rampCount = 0
        triangleCount = 0
        wallQuadCount = 0
        installedFloorMinY = Double.NaN
        installedFloorMaxY = Double.NaN
        debugMesh = null
        debugEpoch++
    }

    /**
     * World-Y range of upward-facing triangles.
     *
     * This is the supporting surface, not the mesh AABB. A wall can make the
     * body AABB much taller than the floor the vehicle is supposed to rest on.
     */
    private fun upwardFloorRange(plan: TerrainMeshPlan): Pair<Double, Double>? {
        val vertices = plan.vertices
        val indices = plan.indices
        if (indices.size < 3 || vertices.size < 9) return null
        var minTop = Double.POSITIVE_INFINITY
        var maxTop = Double.NEGATIVE_INFINITY
        var found = false
        var cursor = 0
        while (cursor + 2 < indices.size) {
            val a = indices[cursor]
            val b = indices[cursor + 1]
            val c = indices[cursor + 2]
            cursor += 3
            val a3 = a * 3
            val b3 = b * 3
            val c3 = c * 3
            if (a3 < 0 || c3 + 2 >= vertices.size) continue
            val ax = vertices[a3].toDouble()
            val ay = vertices[a3 + 1].toDouble()
            val az = vertices[a3 + 2].toDouble()
            val bx = vertices[b3].toDouble()
            val by = vertices[b3 + 1].toDouble()
            val bz = vertices[b3 + 2].toDouble()
            val cx = vertices[c3].toDouble()
            val cy = vertices[c3 + 1].toDouble()
            val cz = vertices[c3 + 2].toDouble()
            val nx = (by - ay) * (cz - az) - (bz - az) * (cy - ay)
            val ny = (bz - az) * (cx - ax) - (bx - ax) * (cz - az)
            val nz = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
            if (ny <= 0.0 || ny * ny <= 0.25 * (nx * nx + ny * ny + nz * nz)) continue
            val top = plan.originY + maxOf(ay, by, cy)
            minTop = minOf(minTop, top)
            maxTop = maxOf(maxTop, top)
            found = true
        }
        return if (found) minTop to maxTop else null
    }

    private fun createBody(plan: TerrainMeshPlan): PhysicsRigidBody {
        val count = plan.vertices.size / 3
        val points = Array(count) { index ->
            Vector3f(
                plan.vertices[index * 3],
                plan.vertices[index * 3 + 1],
                plan.vertices[index * 3 + 2],
            )
        }
        val mesh = IndexedMesh(points, plan.indices)
        val shape = MeshCollisionShape(true, mesh).apply { margin = .001f }
        val proxy = PhysicsRigidBody(shape, 0f).apply {
            name = "sbw_tracked_terrain_${vehicleId}_${plan.contentHash}"
            owner = host
            collisionGroup = TrackedTerrainCollision.TERRAIN_MESH
            setCollideWithGroups(CollisionGroups.NONE)
            collideWithOwnerGroups = CollisionGroups.CHASSIS or CollisionGroups.WHEEL or CollisionGroups.TRACK_LINK
            // Match the previous terrain body's friction. Per-contact friction
            // is written by the belt contact layer anyway; this value only
            // applies before that first write.
            friction = .5f
            restitution = 0f
            setPhysicsLocation(Vector3f(plan.originX.toFloat(), plan.originY.toFloat(), plan.originZ.toFloat()))
        }
        TrackedTerrainCollision.register(proxy, host)
        return proxy
    }
}
