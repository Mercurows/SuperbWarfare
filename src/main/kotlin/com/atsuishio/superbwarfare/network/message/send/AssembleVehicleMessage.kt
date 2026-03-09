package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.Serializable

@Serializable
data class AssembleVehicleMessage(val id: SerializedResourceLocation, val containerId: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val menu = player.containerMenu as? VehicleAssemblingMenu ?: return

        if (menu.containerId != containerId) return
        menu.assembleVehicle(id, player)
    }
}
