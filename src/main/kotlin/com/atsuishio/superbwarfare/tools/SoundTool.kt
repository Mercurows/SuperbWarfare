package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.network.message.receive.SoundClientMessage
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.*

fun Player?.playLocalSound(sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
    SoundTool.playLocalSound(this, sound, volume, pitch)
}

fun ServerPlayer.playLocalSound(
    sound: SoundEvent,
    source: SoundSource = SoundSource.PLAYERS,
    volume: Float = 1f,
    pitch: Float = 1f
) {
    SoundTool.playLocalSound(this, sound, source, volume, pitch)
}

fun ServerPlayer.stopSound(sound: ResourceLocation?, source: SoundSource? = SoundSource.PLAYERS) {
    SoundTool.stopSound(this, sound, source)
}

object SoundTool {
    @JvmStatic
    @JvmOverloads
    fun playLocalSound(player: Player?, sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
        if (player is ServerPlayer) {
            SoundTool.playLocalSound(player, sound, volume, pitch)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun playLocalSound(player: ServerPlayer, sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
        playLocalSound(player, sound, SoundSource.PLAYERS, volume, pitch)
    }

    @JvmStatic
    fun playLocalSound(player: ServerPlayer, sound: SoundEvent, source: SoundSource, volume: Float, pitch: Float) {
        sendPacketTo(
            player, ClientboundSoundPacket(
                Holder.Direct(sound),
                source, player.x, player.y, player.z, volume, pitch, player.level().random.nextLong()
            )
        )
    }

    @JvmStatic
    @JvmOverloads
    fun stopSound(player: ServerPlayer, sound: ResourceLocation?, source: SoundSource? = SoundSource.PLAYERS) {
        sendPacketTo(player, ClientboundStopSoundPacket(sound, source))
    }

    @JvmStatic
    fun playDistantSound(
        serverLevel: ServerLevel,
        soundEvent: SoundEvent,
        pos: Vec3,
        radius: Float,
        pitch: Float,
        sender: Entity?
    ) {
        val players = serverLevel.getPlayers { it.distanceToSqr(pos) < radius * radius * 256 }
        for (serverPlayer in players) {
            sendPacketTo(
                serverPlayer,
                SoundClientMessage(
                    soundEvent.location,
                    pos.x,
                    pos.y,
                    pos.z,
                    radius,
                    pitch,
                    if (sender == null) UUID.randomUUID() else sender.getUUID()
                )
            )
        }
    }
}
