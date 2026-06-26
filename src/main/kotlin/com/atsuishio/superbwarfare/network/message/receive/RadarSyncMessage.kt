package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.Serializable

@Serializable
data class RadarSyncMessage(
    val dim: SerializedResourceLocation,
    val radars: List<SyncedRadar>,
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.syncRadars(dim, radars)
    }

    @Serializable
    data class SyncedRadar(
        val pos: SerializedVec3,
        val radius: Double,
        val sweepAngle: Double,
        val yRot: Double,
        val ownerName: String,
        val showIcon: Boolean = true,
        /** 雷达源唯一标识（如 "block_<pos>" / "vehicle_<id>"），用于替换而非累积 */
        val sourceId: String = "",
    )
}
