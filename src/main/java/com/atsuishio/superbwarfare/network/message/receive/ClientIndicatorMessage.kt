package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.network.PacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class ClientIndicatorMessage(
    val messageType: Int,
    val value: Int,
) : PacketPayload<ClientIndicatorMessage>() {

    override fun handler(message: ClientIndicatorMessage, context: PayloadContext) {
        // TODO: 这样处理是否存在服务端执行问题？
        when (message.messageType) {
            1 -> CrossHairOverlay.headIndicator = message.value
            2 -> CrossHairOverlay.killIndicator = message.value
            3 -> CrossHairOverlay.vehicleIndicator = message.value
            else -> CrossHairOverlay.hitIndicator = message.value
        }
    }
}
