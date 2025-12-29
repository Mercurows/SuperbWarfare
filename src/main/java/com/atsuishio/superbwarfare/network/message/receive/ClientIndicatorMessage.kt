package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.network.PacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class ClientIndicatorMessage(
    val type: Int,
    val value: Int,
) : PacketPayload<ClientIndicatorMessage>() {

    override fun PayloadContext.handler() {
        when (type) {
            1 -> CrossHairOverlay.headIndicator = value
            2 -> CrossHairOverlay.killIndicator = value
            3 -> CrossHairOverlay.vehicleIndicator = value
            else -> CrossHairOverlay.hitIndicator = value
        }
    }
}
