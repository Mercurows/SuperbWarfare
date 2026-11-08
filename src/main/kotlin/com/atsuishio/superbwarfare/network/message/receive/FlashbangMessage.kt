package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.overlay.FlashbangOverlay
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class FlashbangMessage(val visual: Float, val audio: Float) : ClientPacketPayload() {
    override fun PayloadContext.handler() = FlashbangOverlay.apply(visual, audio)
}
