package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.mc

@RegisterPacket
object OpenTacticalMapScreenMessage : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        mc.setScreen(TacticalMapScreen())
    }
}
