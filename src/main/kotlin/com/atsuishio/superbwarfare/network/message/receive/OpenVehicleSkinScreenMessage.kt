package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.VehicleSkinScreen
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.Serializable

@Serializable
data class OpenVehicleSkinScreenMessage(val entityId: Int) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(entityId) ?: return
        mc.setScreen(VehicleSkinScreen(entity))
    }
}
