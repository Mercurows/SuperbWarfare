package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class BlueprintSetIndexMessage(val index: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = this.sender()
        val menu = player.containerMenu as? BlueprintResearchTableMenu ?: return
        if (!menu.stillValid(player)) return

        menu.setLastSelectedIndex(index)
    }
}