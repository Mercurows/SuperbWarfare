package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.perk.Perk
import kotlinx.serialization.Serializable

@Serializable
data class SetPerkLevelMessage(val msgType: Int, val add: Boolean) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val menu = player.containerMenu as? ReforgingTableMenu ?: return
        if (!menu.stillValid(player)) return

        menu.setPerkLevel(Perk.Type.entries[msgType], add, player.abilities.instabuild)
    }
}
