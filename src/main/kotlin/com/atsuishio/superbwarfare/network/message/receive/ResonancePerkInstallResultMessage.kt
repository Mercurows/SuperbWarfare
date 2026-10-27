package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.ResonanceReforgingScreen
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class ResonancePerkInstallResultMessage(val requestId: Long, val success: Boolean, val message: String) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        (mc.screen as? ResonanceReforgingScreen)?.onInstallResult(requestId, success, message)
    }
}
