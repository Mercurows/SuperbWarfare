package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

@RegisterPacket
object BlueprintCraftMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = this.sender()
        val menu = player.containerMenu as? BlueprintResearchTableMenu ?: return
        if (!menu.stillValid(player)) return

        menu.setActivated(true)
    }
}
