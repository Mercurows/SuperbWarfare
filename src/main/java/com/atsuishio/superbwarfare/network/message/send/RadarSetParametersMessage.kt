package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class RadarSetParametersMessage(val mode: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val menu = player.containerMenu as? FuMO25Menu ?: return
        if (!menu.stillValid(player)) return

        menu.setPosToParameters()
    }
}