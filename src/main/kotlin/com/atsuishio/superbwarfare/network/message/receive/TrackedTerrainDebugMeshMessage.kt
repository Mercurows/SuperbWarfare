package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.debug.TrackedTerrainDebugClient
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainDebugMesh
import com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainDebugSelection
import net.minecraft.network.FriendlyByteBuf

/**
 * One chunk of the triangle mesh installed in Bullet for a tracked vehicle.
 *
 * The vertices are already in world coordinates. A chunk is display data only:
 * the client must not turn it back into a collision shape.
 */
class TrackedTerrainDebugMeshMessage(
    val entityId: Int,
    val clear: Boolean,
    val requestId: Long,
    val contentHash: Long,
    val chunkIndex: Int,
    val chunkCount: Int,
    val truncated: Boolean,
    val totalTriangles: Int,
    val triangles: FloatArray,
) : ClientPacketPayload() {

    fun writeTo(buf: FriendlyByteBuf) {
        buf.writeVarInt(entityId)
        buf.writeBoolean(clear)
        if (clear) return
        buf.writeLong(requestId)
        buf.writeLong(contentHash)
        buf.writeVarInt(chunkIndex)
        buf.writeVarInt(chunkCount)
        buf.writeBoolean(truncated)
        buf.writeVarInt(totalTriangles)
        buf.writeVarInt(triangles.size / TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE)
        for (value in triangles) buf.writeFloat(value)
    }

    override fun PayloadContext.handler() {
        TrackedTerrainDebugClient.accept(this@TrackedTerrainDebugMeshMessage)
    }

    companion object {
        fun clear(entityId: Int) = TrackedTerrainDebugMeshMessage(
            entityId, true, 0L, 0L, 0, 1, false, 0, FloatArray(0),
        )

        fun chunks(
            entityId: Int,
            requestId: Long,
            contentHash: Long,
            selection: TrackedTerrainDebugSelection,
        ): List<TrackedTerrainDebugMeshMessage> {
            val width = TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE
            val count = selection.triangleCount
            if (count == 0) {
                return listOf(
                    TrackedTerrainDebugMeshMessage(
                        entityId, false, requestId, contentHash, 0, 1,
                        selection.truncated, selection.totalTriangles, FloatArray(0),
                    ),
                )
            }
            val chunkSize = TrackedTerrainDebugMesh.CHUNK_TRIANGLES
            val chunks = (count + chunkSize - 1) / chunkSize
            return (0 until chunks).map { chunk ->
                val start = chunk * chunkSize
                val end = minOf(count, start + chunkSize)
                TrackedTerrainDebugMeshMessage(
                    entityId = entityId,
                    clear = false,
                    requestId = requestId,
                    contentHash = contentHash,
                    chunkIndex = chunk,
                    chunkCount = chunks,
                    truncated = selection.truncated,
                    totalTriangles = selection.totalTriangles,
                    triangles = selection.packed.copyOfRange(start * width, end * width),
                )
            }
        }

        @JvmStatic
        fun readFrom(buf: FriendlyByteBuf): TrackedTerrainDebugMeshMessage {
            val entityId = buf.readVarInt()
            val clear = buf.readBoolean()
            if (clear) return clear(entityId)
            val requestId = buf.readLong()
            val contentHash = buf.readLong()
            val chunkIndex = buf.readVarInt()
            val chunkCount = buf.readVarInt()
            val truncated = buf.readBoolean()
            val total = buf.readVarInt()
            val triangleCount = buf.readVarInt().coerceIn(0, TrackedTerrainDebugMesh.CHUNK_TRIANGLES)
            val triangles = FloatArray(triangleCount * TrackedTerrainDebugMesh.FLOATS_PER_TRIANGLE)
            for (index in triangles.indices) triangles[index] = buf.readFloat()
            return TrackedTerrainDebugMeshMessage(
                entityId, false, requestId, contentHash, chunkIndex, chunkCount, truncated, total, triangles,
            )
        }
    }
}
