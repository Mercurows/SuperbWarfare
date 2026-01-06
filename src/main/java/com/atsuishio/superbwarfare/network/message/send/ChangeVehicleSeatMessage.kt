package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class ChangeVehicleSeatMessage(val index: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val vehicle = player.vehicle as? VehicleEntity ?: return
        vehicle.changeSeat(player, index)
    }
}
