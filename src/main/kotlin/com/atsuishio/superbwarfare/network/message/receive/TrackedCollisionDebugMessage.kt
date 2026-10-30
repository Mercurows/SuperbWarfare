package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.debug.TrackedTerrainDebugClient
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.physics.tracked.TrackedCollisionDebug
import net.minecraft.network.FriendlyByteBuf

/**
 * One chunk of collision-shape edges for a tracked vehicle.
 *
 * The edges are already in world coordinates. They are the hull OBBs, wheel
 * cylinders and run boxes that were installed, not a second collision shape.
 */
class TrackedCollisionDebugMessage(
    val entityId: Int,
    val clear: Boolean,
    val epoch: Long,
    val chunkIndex: Int,
    val chunkCount: Int,
    val truncated: Boolean,
    val segments: FloatArray,
) : ClientPacketPayload() {

    fun writeTo(buf: FriendlyByteBuf) {
        buf.writeVarInt(entityId)
        buf.writeBoolean(clear)
        if (clear) return
        buf.writeLong(epoch)
        buf.writeVarInt(chunkIndex)
        buf.writeVarInt(chunkCount)
        buf.writeBoolean(truncated)
        buf.writeVarInt(segments.size / TrackedCollisionDebug.FLOATS_PER_SEGMENT)
        for (value in segments) buf.writeFloat(value)
    }

    override fun PayloadContext.handler() {
        TrackedTerrainDebugClient.acceptShapes(this@TrackedCollisionDebugMessage)
    }

    companion object {
        fun clear(entityId: Int) = TrackedCollisionDebugMessage(
            entityId, true, 0L, 0, 1, false, FloatArray(0),
        )

        fun chunks(entityId: Int, snapshot: TrackedCollisionDebug.Snapshot): List<TrackedCollisionDebugMessage> {
            val width = TrackedCollisionDebug.FLOATS_PER_SEGMENT
            val count = snapshot.segmentCount
            if (count == 0) {
                return listOf(
                    TrackedCollisionDebugMessage(
                        entityId, false, snapshot.epoch, 0, 1, snapshot.truncated, FloatArray(0),
                    ),
                )
            }
            val chunkSize = TrackedCollisionDebug.CHUNK_SEGMENTS
            val chunks = (count + chunkSize - 1) / chunkSize
            return (0 until chunks).map { chunk ->
                val start = chunk * chunkSize
                val end = minOf(count, start + chunkSize)
                TrackedCollisionDebugMessage(
                    entityId = entityId,
                    clear = false,
                    epoch = snapshot.epoch,
                    chunkIndex = chunk,
                    chunkCount = chunks,
                    truncated = snapshot.truncated,
                    segments = snapshot.segments.copyOfRange(start * width, end * width),
                )
            }
        }

        @JvmStatic
        fun readFrom(buf: FriendlyByteBuf): TrackedCollisionDebugMessage {
            val entityId = buf.readVarInt()
            val clear = buf.readBoolean()
            if (clear) return clear(entityId)
            val epoch = buf.readLong()
            val chunkIndex = buf.readVarInt()
            val chunkCount = buf.readVarInt()
            val truncated = buf.readBoolean()
            val count = buf.readVarInt().coerceIn(0, TrackedCollisionDebug.CHUNK_SEGMENTS)
            val segments = FloatArray(count * TrackedCollisionDebug.FLOATS_PER_SEGMENT)
            for (index in segments.indices) segments[index] = buf.readFloat()
            return TrackedCollisionDebugMessage(
                entityId, false, epoch, chunkIndex, chunkCount, truncated, segments,
            )
        }
    }
}
