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

        // lerp这个会让弹射物抽搐(恼

        val dm = entity.deltaMovement
        entity.setDeltaMovement(
            dm.x + (motionX - dm.x),
            dm.y + (motionY - dm.y),
            dm.z + (motionZ - dm.z)
        )
    }
}
