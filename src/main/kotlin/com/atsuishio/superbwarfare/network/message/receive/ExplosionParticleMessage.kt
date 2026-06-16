package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@Serializable
data class ExplosionParticleMessage(
    val type: ParticleTool.ParticleType,
    val x: Double,
    val y: Double,
    val z: Double
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        ParticleTool.spawnExplosionParticlesClient(type, player.level(), Vec3(x, y, z))
    }

    companion object {

        /**
         * Sends the explosion particle message to all players within 1024 blocks.
         */
        @JvmStatic
        fun sendToNearbyPlayers(
            level: ServerLevel,
            type: ParticleTool.ParticleType,
            pos: Vec3
        ) {
            val center = pos
            val radius = 1024.0
            for (player in level.getEntitiesOfClass(
                ServerPlayer::class.java,
                AABB(center, center).inflate(radius)
            ) { true }) {
                player.sendPacket(ExplosionParticleMessage(type, pos.x, pos.y, pos.z))
            }
        }
    }
}
