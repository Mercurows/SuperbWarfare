package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class GunMuzzleFxMessage(val shooter: Int, val weapon: String, val subWeapon: Boolean) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        MuzzleFlashRenderer.remoteShot(shooter, weapon, subWeapon)
    }
}
