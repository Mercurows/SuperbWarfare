package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.menu.ReforgingTableMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

object GunReforgeMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val menu = player.containerMenu
        if (menu is ReforgingTableMenu) {
            if (!menu.stillValid(player)) {
                return
            }
            menu.generateResult()
        }
    }
}