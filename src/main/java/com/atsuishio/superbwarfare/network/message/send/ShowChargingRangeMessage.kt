package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class ShowChargingRangeMessage(val operation: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val menu = player.containerMenu as? ChargingStationMenu ?: return
        if (!menu.stillValid(player)) return

        menu.setShowRange(operation)
    }
}
