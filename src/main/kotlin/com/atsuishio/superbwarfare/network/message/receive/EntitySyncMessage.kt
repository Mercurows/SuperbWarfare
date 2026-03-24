package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.*
import kotlinx.serialization.Serializable

@Serializable
data class EntitySyncMessage(
    val id: Int,
    val dim: SerializedResourceLocation,
    val x: Double,
    val y: Double,
    val z: Double,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        ClientEntityTracker.updateEntity(id, dim, x, y, z)
    }
}