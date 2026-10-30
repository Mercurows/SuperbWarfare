package com.atsuishio.superbwarfare.client.debug

import com.atsuishio.superbwarfare.network.message.receive.TrackedCollisionDebugMessage
import com.atsuishio.superbwarfare.network.message.receive.TrackedTerrainDebugMeshMessage
import com.atsuishio.superbwarfare.physics.tracked.TrackedCollisionDebug
import com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainDebugMesh
import java.util.concurrent.ConcurrentHashMap

/**
 * Client copy of a Bullet terrain mesh.
 *
 * Nothing here is collision. A missing packet means the line view is empty,
 * not that the server mesh was rebuilt.
 */
object TrackedTerrainDebugClient {
    private class Partial(
        val requestId: Long,
        val contentHash: Long,
        val chunkCount: Int,
        val truncated: Boolean,
        val totalTriangles: Int,
        val chunks: Array<FloatArray?>,
    ) {
        fun complete(): Boolean = chunks.all { it != null }

        fun build(): RenderMesh {
            val width = TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE
            val count = chunks.sumOf { (it?.size ?: 0) / width }
            val packed = FloatArray(count * width)
            var cursor = 0
            for (chunk in chunks) {
                if (chunk == null) continue
                chunk.copyInto(packed, cursor)
                cursor += chunk.size
            }
            return RenderMesh(requestId, contentHash, truncated, totalTriangles, packed)
        }
    }

    class RenderMesh(
        val requestId: Long,
        val contentHash: Long,
        val truncated: Boolean,
        val totalTriangles: Int,
        val triangles: FloatArray,
    ) {
        val triangleCount: Int get() = triangles.size / TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE
    }

    private class ShapePartial(
        val epoch: Long,
        val chunkCount: Int,
        val truncated: Boolean,
        val chunks: Array<FloatArray?>,
    ) {
        fun complete(): Boolean = chunks.all { it != null }

        fun build(): ShapeView {
            val width = TrackedCollisionDebug.FLOATS_PER_SEGMENT
            val count = chunks.sumOf { (it?.size ?: 0) / width }
            val packed = FloatArray(count * width)
            var cursor = 0
            for (chunk in chunks) {
                if (chunk == null) continue
                chunk.copyInto(packed, cursor)
                cursor += chunk.size
            }
            return ShapeView(epoch, truncated, packed)
        }
    }

    class ShapeView(
        val epoch: Long,
        val truncated: Boolean,
        val segments: FloatArray,
    ) {
        val segmentCount: Int get() = segments.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT
    }

    private val partials = ConcurrentHashMap<Int, Partial>()
    private val ready = ConcurrentHashMap<Int, RenderMesh>()
    private val shapePartials = ConcurrentHashMap<Int, ShapePartial>()
    private val shapes = ConcurrentHashMap<Int, ShapeView>()

    fun accept(message: TrackedTerrainDebugMeshMessage) {
        if (message.clear) {
            partials.remove(message.entityId)
            ready.remove(message.entityId)
            return
        }
        if (message.chunkCount <= 0 || message.chunkIndex !in 0 until message.chunkCount) return
        val current = partials[message.entityId]
        val partial = if (
            current == null ||
            current.requestId != message.requestId ||
            current.contentHash != message.contentHash ||
            current.chunkCount != message.chunkCount
        ) {
            Partial(
                message.requestId,
                message.contentHash,
                message.chunkCount,
                message.truncated,
                message.totalTriangles,
                arrayOfNulls(message.chunkCount),
            ).also { partials[message.entityId] = it }
        } else {
            current
        }
        if (message.chunkIndex < partial.chunks.size) partial.chunks[message.chunkIndex] = message.triangles
        if (partial.complete()) {
            ready[message.entityId] = partial.build()
            partials.remove(message.entityId)
        }
    }

    fun acceptShapes(message: TrackedCollisionDebugMessage) {
        if (message.clear) {
            shapePartials.remove(message.entityId)
            shapes.remove(message.entityId)
            return
        }
        if (message.chunkCount <= 0 || message.chunkIndex !in 0 until message.chunkCount) return
        val current = shapePartials[message.entityId]
        val partial = if (
            current == null ||
            current.epoch != message.epoch ||
            current.chunkCount != message.chunkCount
        ) {
            ShapePartial(
                message.epoch,
                message.chunkCount,
                message.truncated,
                arrayOfNulls(message.chunkCount),
            ).also { shapePartials[message.entityId] = it }
        } else {
            current
        }
        if (message.chunkIndex < partial.chunks.size) partial.chunks[message.chunkIndex] = message.segments
        if (partial.complete()) {
            shapes[message.entityId] = partial.build()
            shapePartials.remove(message.entityId)
        }
    }

    fun meshCount(): Int = ready.size

    fun shapeCount(): Int = shapes.size

    fun forEach(draw: (RenderMesh) -> Unit) {
        for (mesh in ready.values) draw(mesh)
    }

    fun forEachShape(draw: (ShapeView) -> Unit) {
        for (shape in shapes.values) draw(shape)
    }

    fun clearAll() {
        partials.clear()
        ready.clear()
        shapePartials.clear()
        shapes.clear()
    }
}
