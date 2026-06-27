package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.item.weapon.BeastItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class EntityClearMessage(val entityId: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (!player.hasPermissions(2)) return
        // Schedule on main server thread to ensure particles, sounds and
        // entity removal all run on the correct logical side.
        player.server.execute {
            val target = player.level().getEntity(entityId) ?: return@execute
            BeastItem.beastKill(player, target)
        }
    }
}
