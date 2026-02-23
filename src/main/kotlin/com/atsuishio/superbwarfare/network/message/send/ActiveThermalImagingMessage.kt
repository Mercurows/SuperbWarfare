package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModAttachments
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class ActiveThermalImagingMessage(val active: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch()
        cap.activeThermalImaging = active
        cap.sync(player)
    }
}
