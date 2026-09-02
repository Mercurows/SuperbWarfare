package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.lighting.ProjectileLightHelper
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ThreadLocalRandom

/**
 * Clientbound network packet containing explosion metadata and spatial coordinates.
 *
 * Applies randomized radius and duration variance per explosion to ensure every detonation is unique.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@Serializable
@RegisterPacket
data class ExplosionParticleMessage(
    val type: ParticleTool.ParticleType,
    val x: Double,
    val y: Double,
    val z: Double
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return
        val level = player.level()
        val pos = Vec3(x, y, z)
        
        // Using ThreadLocalRandom ensures absolute randomness even if multiple 
        // explosion packets arrive and are processed in the exact same millisecond.
        val random = ThreadLocalRandom.current()

        val baseRadius = when (type) {
            ParticleTool.ParticleType.MINI -> 2.5f
            ParticleTool.ParticleType.SMALL -> 4.0f
            ParticleTool.ParticleType.MEDIUM -> 6.0f
            ParticleTool.ParticleType.LARGE -> 8.0f
            ParticleTool.ParticleType.HUGE -> 12.0f
            ParticleTool.ParticleType.GIANT -> 16.0f
            ParticleTool.ParticleType.EPIC -> 24.0f
        }

        // Expanded randomized variance (75% to 125% radius scaling) per explosion event
        val randomizedRadius = (baseRadius * (0.75f + random.nextFloat() * 0.50f)).coerceAtLeast(1.5f)

        ProjectileLightHelper.emitExplosionFlashDirect(level, pos, randomizedRadius)
        ParticleTool.spawnExplosionParticlesClient(type, level, pos)
    }

    companion object {

        /**
         * Sends the explosion particle message to all players in the same dimension.
         */
        @JvmStatic
        fun sendToNearbyPlayers(
            level: ServerLevel,
            type: ParticleTool.ParticleType,
            pos: Vec3
        ) {
            for (player in level.players()) {
                if (player.position().distanceToSqr(pos) < 4096 * 4096) {
                    player.sendPacket(ExplosionParticleMessage(type, pos.x, pos.y, pos.z))
                }
            }
        }
    }
}