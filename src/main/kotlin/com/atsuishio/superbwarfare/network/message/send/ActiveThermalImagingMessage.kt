package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class ActiveThermalImagingMessage(val active: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        PlayerVariable.modify(player) { it.activeThermalImaging = active }
    }
}
