package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance

@Serializable
data class PlayerStopRidingMessage(val ejection: Boolean) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return

        if (ejection) {
            val vec = vehicle.getEjectionMovement(player, vehicle.getTagSeatIndex(player))
            val pos = vehicle.getEjectionPosition(player, vehicle.getTagSeatIndex(player))
            val level = player.level()
            level.playSound(
                null,
                player.x, player.y, player.z,
                ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.PLAYERS,
                4f, 1f
            )
            if (level is ServerLevel) {
                for (p in 0..7) {
                    val pPos = player.position().add(vec.scale(p * 0.5))
                    ParticleTool.sendParticle(
                        level, ParticleTypes.CLOUD,
                        pPos.x, pPos.y, pPos.z,
                        10, 0.5, 0.5, 0.5, 0.05, true
                    )
                    ParticleTool.sendParticle(
                        level, ParticleTypes.FLAME,
                        pPos.x, pPos.y, pPos.z,
                        20, 0.5, 0.5, 0.5, 0.05, true
                    )
                    ParticleTool.sendParticle(
                        level, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pPos.x, pPos.y, pPos.z,
                        15, 0.5, 0.5, 0.5, 0.05, true
                    )
                }
            }

            queueServerWork(1) {
                player.sendPacket(ClientSetMotionMessage(vec.toVector3f(), pos.toVector3f()))
            }
        }

        player.stopRiding()
        player.setJumping(false)

        player.addEffect(MobEffectInstance(ModMobEffects.STRIKE_PROTECTION.get(), 10, 0, false, false), player)
    }
}
