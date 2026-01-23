package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext

object DrawClientMessage : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientEventHandler.resetGunStatus()
    }
}
