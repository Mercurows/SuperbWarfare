package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.PacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.toVec3
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
data class ClientSetMotionMessage(
    @Contextual val motion: Vector3f,
    @Contextual val position: Vector3f,
) : PacketPayload<ClientSetMotionMessage>() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return

        player.setPos(position.toVec3())
        player.deltaMovement = motion.toVec3()
    }
}
