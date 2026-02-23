package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

object GunReforgeMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val menu = player.containerMenu as? ReforgingTableMenu ?: return
        if (!menu.stillValid(player)) return

        menu.generateResult()
    }
}