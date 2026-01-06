package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
data class ChangeVehicleSeatMessage(val index: Int) : PacketPayload() {
    override fun PayloadContext.handler() {
        val player = player() as ServerPlayer

        val vehicle = player.vehicle as? VehicleEntity ?: return
        vehicle.changeSeat(player, index)
    }
}
