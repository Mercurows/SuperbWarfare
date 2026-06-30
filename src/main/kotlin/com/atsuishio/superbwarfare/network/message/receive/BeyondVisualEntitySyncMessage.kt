package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.Serializable

/**
 * 超视距世界渲染同步包。
 *
 * 服务端每 [SYNC_INTERVAL_TICKS] tick 无条件将 [ServerSyncedEntityHandler]
 * 中的所有载具/导弹实体发送给同维度的所有玩家，不依赖雷达/IFF/敌我判定。
 * 客户端收到后直接存入 [ClientSyncedEntityHandler.SYNCED_WORLD_RENDER]。
 */
@Serializable
data class BeyondVisualEntitySyncMessage(
    val dim: SerializedResourceLocation,
    val list: List<SyncedEntity>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.syncWorldRender(dim, list)
    }
}
