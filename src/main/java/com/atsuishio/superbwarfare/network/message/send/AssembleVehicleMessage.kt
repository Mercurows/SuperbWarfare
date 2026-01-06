package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu
import com.atsuishio.superbwarfare.network.PacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

@Serializable
data class AssembleVehicleMessage(@Contextual val id: ResourceLocation, val containerId: Int) : PacketPayload() {
    override fun PayloadContext.handler() {
        val player = player() as ServerPlayer
        val menu = player.containerMenu as? VehicleAssemblingMenu ?: return

        if (menu.containerId != containerId) return
        menu.assembleVehicle(id, player)
    }
}
