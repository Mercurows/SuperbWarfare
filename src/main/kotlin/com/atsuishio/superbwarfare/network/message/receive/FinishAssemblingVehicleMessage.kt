package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.Serializable

@Serializable
data class FinishAssemblingVehicleMessage(val containerId: Int) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        if (player.containerMenu.containerId != containerId) return

        val screen = mc.screen
        if (screen is VehicleAssemblingScreen) {
            screen.finishAssembling()
        }
    }
}
