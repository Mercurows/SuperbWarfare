package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedBlockPos
import kotlinx.serialization.Serializable

@Serializable
data class RadarMenuOpenMessage(var pos: SerializedBlockPos) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        FuMO25ScreenHelper.resetEntities()
        FuMO25ScreenHelper.pos = pos
    }
}
