package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.animal.Pig

@Serializable
data class SeekingWeaponWarningMessage(val lockOn: Boolean, val uuid: SerializedUUID) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val entity = EntityFindUtil.findEntity(player.level(), uuid.toString()) ?: return

        entity.level().playSound(
            null, entity.onPos,
            if (entity is Pig) SoundEvents.PIG_HURT else if (lockOn) ModSounds.LOCKED_WARNING.get() else ModSounds.LOCKING_WARNING.get(),
            SoundSource.PLAYERS,
            2f, 1f
        )
    }
}