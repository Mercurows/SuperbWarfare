package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.TraceTool
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth

@Serializable
data class AdjustMortarAngleMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val looking = TraceTool.findLookingEntity(player, 6.0) as? MortarEntity ?: return

        looking.getEntityData().set(
            MortarEntity.TARGET_PITCH,
            Mth.clamp(looking.getEntityData().get(MortarEntity.TARGET_PITCH) + 0.5 * scroll, -89.0, -20.0).toFloat()
        )

        SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
    }
}
