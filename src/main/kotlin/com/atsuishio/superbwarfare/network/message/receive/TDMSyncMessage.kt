package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import kotlinx.serialization.Serializable

@Serializable
@RegisterPacket
data class TDMSyncMessage(@JvmField val data: Set<String>) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        ClientEventHandler.tdmSavedData = TDMSavedData(data)
    }
}
