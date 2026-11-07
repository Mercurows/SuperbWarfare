package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.renderer.special.LightningArcRenderer
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@RegisterPacket
@Serializable
data class LightningArcMessage(
    val startX: Double, val startY: Double, val startZ: Double,
    val endX: Double, val endY: Double, val endZ: Double,
    val seed: Long,
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        LightningArcRenderer.add(Vec3(startX, startY, startZ), Vec3(endX, endY, endZ), seed)
    }
}
