package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

@Serializable
data class AssembleVehicleMessage(@Contextual val id: ResourceLocation, val containerId: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val menu = player.containerMenu as? VehicleAssemblingMenu ?: return

        if (menu.containerId != containerId) return
        menu.assembleVehicle(id, player)
    }
}
