package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedBlockPos
import kotlinx.serialization.Serializable

@Serializable
data class RadarSetPosMessage(val pos: SerializedBlockPos) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val menu = player.containerMenu as? FuMO25Menu ?: return
        if (!menu.stillValid(player)) return

        menu.setPos(pos.x, pos.y, pos.z)
    }
}
