package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class ClientIndicatorMessage(
    val type: Int,
    val value: Int,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        // TODO: 这样处理是否存在服务端执行问题？
        when (type) {
            1 -> CrossHairOverlay.headIndicator = value
            2 -> CrossHairOverlay.killIndicator = value
            3 -> CrossHairOverlay.vehicleIndicator = value
            else -> CrossHairOverlay.hitIndicator = value
        }
    }
}
