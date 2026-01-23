package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.atsuishio.superbwarfare.tools.queueClientWorkIfDelayed
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.client.CameraType
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import java.util.*

@Serializable
data class SoundClientMessage(
    @Contextual val location: ResourceLocation,
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Float,
    val pitch: Float,
    @Contextual val sender: UUID,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        if (player.getUUID() == sender && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) return

        val sound = SoundEvent.createVariableRangeEvent(location)
        val distance = player.position().distanceTo(Vec3(x, y, z))

        queueClientWorkIfDelayed((distance / 17).toInt()) {
            player.level().playSound(player, x, y, z, sound, SoundSource.BLOCKS, radius, pitch)
        }
    }
}
