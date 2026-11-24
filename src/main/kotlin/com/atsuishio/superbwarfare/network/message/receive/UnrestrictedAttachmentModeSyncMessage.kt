package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class UnrestrictedAttachmentModeSyncMessage(val enabled: Boolean) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        if (get().networkManager.isMemoryConnection || !SERVER_CONFIG.isLoaded) return
        GunConfig.UNRESTRICTED_ATTACHMENT_MODE.set(enabled)
    }
}
