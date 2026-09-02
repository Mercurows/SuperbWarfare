package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class PlayerInfoSyncMessage(
    val dim: SerializedResourceLocation,
    val list: List<SyncedPlayerInfo>
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.syncPlayerInfo(dim, list)
    }

    @Serializable
    data class SyncedPlayerInfo(
        val uuid: SerializedUUID,
        val pos: SerializedVec3,
        val name: String,
        val onVehicle: Boolean,
        val isDriver: Boolean,
        /** 关系标识："friendly" / "hostile" / "neutral" */
        val relation: String = "friendly",
        /** 服务端实体 ID，用于管理员清除等操作（-1 表示未知） */
        val entityId: Int = -1,
    )

}
