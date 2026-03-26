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
    val friendly: Boolean
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientSyncedEntityHandler.sync(dim, list, friendly)
    }

    @Serializable
    data class SyncedEntity(
        val id: Int,
        val type: SerializedResourceLocation,
        val pos: SerializedVec3,
        val motion: SerializedVec3,
        val tag: SerializedTag
    )
}