package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundSource
import org.joml.Math
import java.util.*

@Serializable
data class MeleeAttackMessage(@Contextual val uuid: UUID) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val lookingEntity = EntityFindUtil.findEntity(player.level(), uuid.toString())
        if (lookingEntity != null) {
            player.level().playSound(
                null,
                lookingEntity.onPos,
                ModSounds.MELEE_HIT.get(),
                SoundSource.PLAYERS,
                1f,
                ((2 * Math.random() - 1) * 0.1f + 1.0f).toFloat()
            )
            player.attack(lookingEntity)
        }
    }
}