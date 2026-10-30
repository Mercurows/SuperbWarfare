package com.atsuishio.superbwarfare.physics.tracked

/**
 * Detached copy of the triangle mesh actually installed as a Bullet
 * `MeshCollisionShape`.
 *
 * The arrays are copies. A renderer or a network packet must not be handed the
 * planner's live buffers, and this object holds no native pointer.
 */
class TrackedTerrainDebugMesh(
    val vehicleId: String,
    val requestId: Long,
    val contentHash: Long,
    val originX: Double,
    val originY: Double,
    val originZ: Double,
    val vertices: FloatArray,
    val indices: IntArray,
    val rampTriangles: BooleanArray,
) {
    val triangleCount: Int get() = indices.size / 3

    fun finite(): Boolean =
        originX.isFinite() && originY.isFinite() && originZ.isFinite() &&
            vertices.all { it.isFinite() } &&
            rampTriangles.size == triangleCount &&
            indices.size % 3 == 0 &&
            indices.all { it >= 0 && it * 3 + 2 < vertices.size }

    companion object {
        const val FLOATS_PER_TRIANGLE = 10
        const val CHUNK_TRIANGLES = 1_200
        const val MAX_TRIANGLES = 24_000

        fun from(vehicleId: String, plan: TerrainMeshPlan): TrackedTerrainDebugMesh {
            val triangles = plan.indices.size / 3
            val flags = if (plan.rampTriangles.size == triangles) {
                plan.rampTriangles.copyOf()
            } else {
                BooleanArray(triangles)
            }
            return TrackedTerrainDebugMesh(
                vehicleId = vehicleId,
                requestId = plan.requestId,
                contentHash = plan.contentHash,
                originX = plan.originX,
                originY = plan.originY,
                originZ = plan.originZ,
                vertices = plan.vertices.copyOf(),
                indices = plan.indices.copyOf(),
                rampTriangles = flags,
            )
        }
    }
}

/**
 * World-space triangles selected for the debug view.
 *
 * [packed] holds ten floats per triangle: three world vertices, then `1` when
 * that triangle was generated as a ramp. Selection keeps the triangles nearest
 * the vehicle when the installed mesh is larger than the packet budget. It does
 * not rebuild or simplify the surface.
 */
class TrackedTerrainDebugSelection(
    val packed: FloatArray,
    val totalTriangles: Int,
    val truncated: Boolean,
) {
    val triangleCount: Int get() = packed.size / TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE

    companion object {
        fun of(
            mesh: TrackedTerrainDebugMesh,
            focusX: Double,
            focusY: Double,
            focusZ: Double,
        ): TrackedTerrainDebugSelection {
            val width = TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE
            val total = mesh.triangleCount
            val order = ArrayList<Int>(total)
            val distance = DoubleArray(total)
            for (triangle in 0 until total) {
                val base = triangle * 3
                val i0 = mesh.indices[base]
                val i1 = mesh.indices[base + 1]
                val i2 = mesh.indices[base + 2]
                if (!mesh.vertexOk(i0) || !mesh.vertexOk(i1) || !mesh.vertexOk(i2)) continue
                val cx = mesh.originX + (mesh.vertices[i0 * 3] + mesh.vertices[i1 * 3] + mesh.vertices[i2 * 3]) / 3.0
                val cy = mesh.originY + (mesh.vertices[i0 * 3 + 1] + mesh.vertices[i1 * 3 + 1] + mesh.vertices[i2 * 3 + 1]) / 3.0
                val cz = mesh.originZ + (mesh.vertices[i0 * 3 + 2] + mesh.vertices[i1 * 3 + 2] + mesh.vertices[i2 * 3 + 2]) / 3.0
                order += triangle
                distance[triangle] = (cx - focusX) * (cx - focusX) + (cy - focusY) * (cy - focusY) + (cz - focusZ) * (cz - focusZ)
            }
            val truncated = order.size > TrackedTerrainDebugMesh.MAX_TRIANGLES
            if (truncated) order.sortBy { distance[it] }
            val kept = order.take(TrackedTerrainDebugMesh.MAX_TRIANGLES)
            val packed = FloatArray(kept.size * width)
            for ((slot, triangle) in kept.withIndex()) {
                val base = triangle * 3
                val out = slot * width
                writeWorld(mesh, mesh.indices[base], packed, out)
                writeWorld(mesh, mesh.indices[base + 1], packed, out + 3)
                writeWorld(mesh, mesh.indices[base + 2], packed, out + 6)
                packed[out + 9] = if (triangle < mesh.rampTriangles.size && mesh.rampTriangles[triangle]) 1f else 0f
            }
            return TrackedTerrainDebugSelection(packed, order.size, truncated)
        }

        private fun writeWorld(mesh: TrackedTerrainDebugMesh, index: Int, out: FloatArray, at: Int) {
            out[at] = (mesh.originX + mesh.vertices[index * 3]).toFloat()
            out[at + 1] = (mesh.originY + mesh.vertices[index * 3 + 1]).toFloat()
            out[at + 2] = (mesh.originZ + mesh.vertices[index * 3 + 2]).toFloat()
        }
    }
}

private fun TrackedTerrainDebugMesh.vertexOk(index: Int): Boolean =
    index >= 0 && index * 3 + 2 < vertices.size
