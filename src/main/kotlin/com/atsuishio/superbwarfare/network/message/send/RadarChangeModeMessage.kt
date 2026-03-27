package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class RadarChangeModeMessage(val mode: Byte) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        if (mode !in 1..4) return
        val player = sender()

        val menu = player.containerMenu as? FuMO25Menu ?: return
        if (!player.containerMenu.stillValid(player)) return

        menu.funcType = mode.toLong()
    }
}
