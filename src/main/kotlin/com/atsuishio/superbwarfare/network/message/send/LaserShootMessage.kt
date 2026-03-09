package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundSource

@Serializable
data class LaserShootMessage(
    val damage: Double,
    val uuid: SerializedUUID,
    val headshot: Boolean,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val level = player.level()

        val entity = EntityFindUtil.findEntity(level, uuid.toString()) ?: return

        if (headshot) {
            entity.forceHurt(
                ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), player, player),
                (2 * damage).toFloat()
            )
            player.level()
                .playSound(null, player.blockPosition(), ModSounds.HEADSHOT.get(), SoundSource.VOICE, 0.1f, 1f)
            sendPacketTo(player, ClientIndicatorMessage(1, 5))
        } else {
            entity.forceHurt(
                ModDamageTypes.causeLaserDamage(level.registryAccess(), player, player),
                damage.toFloat()
            )
            player.level()
                .playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1f)
            sendPacketTo(player, ClientIndicatorMessage(0, 5))
        }
        entity.invulnerableTime = 0
    }
}
