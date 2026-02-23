package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

@Serializable
data class ClientMotionSyncMessage(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
) : ClientPacketPayload() {

    constructor(id: Int, motion: Vec3) : this(id, motion.x.toFloat(), motion.y.toFloat(), motion.z.toFloat())
    constructor(entity: Entity) : this(entity.id, entity.deltaMovement)

    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(id) ?: return
        entity.lerpMotion(x.toDouble(), y.toDouble(), z.toDouble())
    }
}
