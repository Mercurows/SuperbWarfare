package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class RemoteBindingSyncMessage(@SerialName("Linked") val linked: Boolean) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientEventHandler.hasRemoteBinding = linked
    }
}
