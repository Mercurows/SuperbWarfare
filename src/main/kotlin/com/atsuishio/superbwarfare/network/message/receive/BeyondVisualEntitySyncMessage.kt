package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedTag
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.Serializable

/**
 * 超视距世界渲染同步包。
 *
 * 服务端每 [SYNC_INTERVAL_TICKS] tick 无条件将 [ServerSyncedEntityHandler]
 * 中的所有载具/导弹实体发送给同维度的所有玩家，不依赖雷达/IFF/敌我判定。
 * 客户端收到后直接存入 [ClientSyncedEntityHandler.SYNCED_WORLD_RENDER]。
 *
 * 这是所有超视距实体状态数据的唯一来源。敌我关系由 [EntityRelationSyncMessage] 的 ID 池判定。
 */
@Serializable
data class BeyondVisualEntitySyncMessage(
    val dim: SerializedResourceLocation,
    val list: List<SyncedEntity>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.syncWorldRender(dim, list)
    }

    @Serializable
    data class SyncedEntity(
        val id: Int,
        val type: SerializedResourceLocation,
        val pos: SerializedVec3,
        val targetPos: SerializedVec3?,
        val tag: SerializedTag,
        val yRot: Float = 0f,
        val xRot: Float = 0f,
        /** 离地高度，-1 表示未计算 */
        val heightAboveGround: Double = -1.0,
        /** 标记为已移除，客户端收到后立即从同步列表中清理 */
        val removed: Boolean = false,
    )
}
