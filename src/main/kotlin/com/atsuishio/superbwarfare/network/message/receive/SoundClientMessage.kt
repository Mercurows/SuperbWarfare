package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.atsuishio.superbwarfare.tools.queueClientWorkIfDelayed
import kotlinx.serialization.Serializable
import net.minecraft.client.CameraType
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

@Serializable
data class SoundClientMessage(
    val location: SerializedResourceLocation,
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Float,
    val pitch: Float,
    val sender: SerializedUUID,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        if (player.uuid == sender && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) return

        val sound = SoundEvent.createVariableRangeEvent(location)
        val distance = player.position().distanceTo(Vec3(x, y, z))

        queueClientWorkIfDelayed((distance / 17).toInt()) {
            player.level().playSound(player, x, y, z, sound, SoundSource.BLOCKS, radius, pitch)
        }
    }
}
