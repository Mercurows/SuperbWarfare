package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

@Serializable
data class MountedGunSoundMessage(
    val vehicleId: Int,
    val position: SerializedVec3,
    val firstPersonSound: SerializedResourceLocation?,
    val thirdPersonSound: SerializedResourceLocation?,
    val firstPersonVolume: Float,
    val thirdPersonVolume: Float,
    val pitch: Float,
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        val firstPerson = player.vehicle?.id == vehicleId &&
            (options.cameraType.isFirstPerson || ClientEventHandler.zoomVehicle)

        if (firstPerson && firstPersonSound != null) {
            player.playSound(SoundEvent.createVariableRangeEvent(firstPersonSound), firstPersonVolume, pitch)
        } else if (thirdPersonSound != null) {
            val sound = SoundEvent.createVariableRangeEvent(thirdPersonSound)
            player.level().playSound(player, position.x, position.y, position.z, sound, SoundSource.PLAYERS, thirdPersonVolume, pitch)
        }
    }
}
