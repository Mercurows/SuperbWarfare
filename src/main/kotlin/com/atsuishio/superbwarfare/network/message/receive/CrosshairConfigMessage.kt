package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class CrosshairConfigMessage(
    val hidden: Boolean
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        CrossHairOverlay.combatHudHidden = hidden
    }
}