package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft

@RegisterPacket
@Serializable
data class GunModificationResultMessage(
    val requestId: String,
    val success: Boolean,
    val reason: String = "",
    val details: List<String> = emptyList(),
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        (Minecraft.getInstance().screen as? WeaponEditScreen)?.onModificationResult(this@GunModificationResultMessage)
    }
}
