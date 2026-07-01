package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.Serializable

/**
 * 轻量级实体身份同步包，只携带 entityId 列表，不携带 NBT/位置等状态数据。
 *
 * 载具/导弹每 tick 将自身 ID 以 [friendlyIds] 发送给友方；
 * 雷达将探测到的敌/中实体 ID 以 [hostileIds]/[neutralIds] 发送给友方。
 * 客户端将 ID 存入三个轻量 ID 池，渲染时从 [ClientSyncedEntityHandler.SYNCED_WORLD_RENDER]
 * 获取实体状态数据，通过 ID 池判定敌我关系。
 */
@Serializable
data class EntityRelationSyncMessage(
    val dim: SerializedResourceLocation,
    val friendlyIds: List<Int> = emptyList(),
    val hostileIds: List<Int> = emptyList(),
    val neutralIds: List<Int> = emptyList(),
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.syncEntityRelations(dim, friendlyIds, hostileIds, neutralIds)
    }
}
