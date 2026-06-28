package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedTag
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.Serializable

@Serializable
data class EntitySyncMessage(
    val dim: SerializedResourceLocation,
    val list: List<SyncedEntity>,
    val friendly: Boolean,
    val neutral: Boolean = false
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.sync(dim, list, friendly, neutral)
    }

    @Serializable
    data class SyncedEntity(
        val id: Int,
        val type: SerializedResourceLocation,
        val pos: SerializedVec3,
        val targetPos: SerializedVec3?,
        val tag: SerializedTag,
        val yRot: Float = 0f,
        /** 离地高度，-1 表示未计算 */
        val heightAboveGround: Double = -1.0,
        /** 标记为已移除，客户端收到后立即从同步列表中清理 */
        val removed: Boolean = false,
    )
}