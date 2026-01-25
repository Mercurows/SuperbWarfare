package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.event.ClientEventHandler.shakeAmplitude
import com.atsuishio.superbwarfare.event.ClientEventHandler.shakePos
import com.atsuishio.superbwarfare.event.ClientEventHandler.shakeRadius
import com.atsuishio.superbwarfare.event.ClientEventHandler.shakeTime
import com.atsuishio.superbwarfare.event.ClientEventHandler.shakeType
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.isNullOrSpector
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.queueClientWorkIfDelayed
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth.DEG_TO_RAD
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@Serializable
data class ShakeClientMessage(
    var time: Double,
    var radius: Double,
    var amplitude: Double,
    var x: Double,
    var y: Double,
    var z: Double
) : ClientPacketPayload() {

    private val shakeStrength by lazy {
        DisplayConfig.EXPLOSION_SCREEN_SHAKE.get().toFloat() / 100.0f
    }

    override fun PayloadContext.handler() {
        val player = localPlayer
        if (player.isNullOrSpector()) return
        if (shakeStrength <= 0.0f) return

        val distance = player.position().distanceTo(Vec3(x, y, z))

        queueClientWorkIfDelayed((distance / 17).toInt()) {
            shakeTime = time
            shakeRadius = radius
            shakeAmplitude = amplitude * DEG_TO_RAD * shakeStrength
            shakePos[0] = x * shakeStrength
            shakePos[1] = y * shakeStrength
            shakePos[2] = z * shakeStrength
            shakeType = 2 * (Math.random() - 0.5)
        }
    }

    companion object {

        @JvmStatic
        fun sendToNearbyPlayers(
            level: Level,
            x: Double,
            y: Double,
            z: Double,
            sendRadius: Double,
            time: Double,
            amplitude: Double
        ) {
            val center = Vec3(x, y, z)

            for (serverPlayer in level.getEntitiesOfClass(
                ServerPlayer::class.java,
                AABB(center, center).inflate(sendRadius)
            ) { true }) {
                serverPlayer.sendPacket(ShakeClientMessage(time, sendRadius, amplitude, x, y, z))
            }
        }

        @JvmStatic
        fun sendToNearbyPlayers(source: Entity, sendRadius: Double, time: Double, amplitude: Double) {
            if (sendRadius <= 0 || time <= 0 || amplitude <= 0) return

            sendToNearbyPlayers(
                source.level(),
                source.x,
                source.y,
                source.z,
                sendRadius,
                time,
                amplitude
            )
        }
    }
}
