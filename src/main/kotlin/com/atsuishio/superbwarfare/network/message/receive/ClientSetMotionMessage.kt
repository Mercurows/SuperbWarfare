package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.toVec3
import kotlinx.serialization.Serializable

@Serializable
data class ClientSetMotionMessage(
    val motion: SerializedVector3f,
    val position: SerializedVector3f,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return

        player.setPos(position.toVec3())
        player.deltaMovement = motion.toVec3()
    }
}
