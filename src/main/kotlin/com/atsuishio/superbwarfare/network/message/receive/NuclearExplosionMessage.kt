package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.renderer.special.NuclearExplosionRenderer
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity.NuclearExplosionParameters
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

@RegisterPacket
@Serializable
data class NuclearExplosionMessage(
    val dimension: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Int,
    val parameters: NuclearExplosionParameters = NuclearExplosionParameters()
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        NuclearExplosionRenderer.spawn(dimension, Vec3(x, y, z), seed, parameters)
    }

    companion object {
        fun send(level: ServerLevel, position: Vec3, parameters: NuclearExplosionParameters = NuclearExplosionParameters()) {
            val effect = parameters.sanitized()
            val packet = NuclearExplosionMessage(level.dimension().location().toString(),
                position.x, position.y, position.z, level.random.nextInt(), effect)
            val range = effect.visibilityRange
            level.players().filter { it.position().distanceToSqr(position) <= range * range }
                .forEach { it.sendPacket(packet) }
        }
    }
}
