package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

@Serializable
data class MissileTrailParticleMessage(
    val xo: Double,
    val yo: Double,
    val zo: Double,
    val bbHeight: Double,
    val motionX: Double,
    val motionY: Double,
    val motionZ: Double
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        val level = player.level()
        val deltaMovement = Vec3(motionX, motionY, motionZ)
        val l = deltaMovement.length()
        val startPos = Vec3(xo, yo + bbHeight / 2, zo)
        var i = 0.0
        while (i < l) {
            val pos = startPos.add(deltaMovement.normalize().scale(-i))
            val random = 2 * (level.random.nextFloat() - 0.5f)
            level.addParticle(
                CustomFlareOption(
                    0.5f, 0.43f, 0.36f, 160, 0.93f,
                    (10 + 8 * random).toInt(), 0.03f
                ), true, pos.x + random * 0.25, pos.y + random * 0.25, pos.z + random * 0.25, 0.0, 0.0, 0.0
            )
            i += 2.0
        }
    }

    companion object {

        /**
         * Sends the missile trail particle message to all players in the same dimension.
         * This allows the huge missile trail to be visible even when the missile entity
         * is beyond the client's normal particle render distance.
         */
        @JvmStatic
        fun sendToNearbyPlayers(
            level: ServerLevel,
            xo: Double, yo: Double, zo: Double,
            bbHeight: Double,
            motionX: Double, motionY: Double, motionZ: Double
        ) {
            for (player in level.players()) {
                // TODO 把4096改成之后最大超视距的距离
                if (player.position().distanceToSqr(Vec3(xo, yo, zo)) < 2048 * 2048) {
                    player.sendPacket(MissileTrailParticleMessage(xo, yo, zo, bbHeight, motionX, motionY, motionZ))
                }
            }
        }
    }
}
