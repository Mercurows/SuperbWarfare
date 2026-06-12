package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.Entity

@Serializable
data class ClientMotionSyncMessage(
    val id: Int,
    val posX: Double,
    val posY: Double,
    val posZ: Double,
    val motionX: Float,
    val motionY: Float,
    val motionZ: Float,
) : ClientPacketPayload() {

    constructor(entity: Entity) : this(
        entity.id,
        entity.x, entity.y, entity.z,
        entity.deltaMovement.x.toFloat(), entity.deltaMovement.y.toFloat(), entity.deltaMovement.z.toFloat()
    )

    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(id) ?: return
        entity.setPos(posX, posY, posZ)

        // 对 motion 做 lerp 插值，避免直接赋值导致的视觉抖动
        // vanilla 的 lerpMotion 在 1.20+ 等同于 setDeltaMovement，没有实际插值
        val lerpFactor = 0.6
        val dm = entity.deltaMovement
        entity.setDeltaMovement(
            dm.x + (motionX - dm.x) * lerpFactor,
            dm.y + (motionY - dm.y) * lerpFactor,
            dm.z + (motionZ - dm.z) * lerpFactor
        )
    }
}
