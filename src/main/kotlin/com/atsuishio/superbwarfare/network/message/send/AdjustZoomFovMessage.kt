package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth

@Serializable
@RegisterPacket
data class AdjustZoomFovMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = player.mainHandItem
        val gun = stack.toGunData() ?: return
        val data = gun.data()

        val scopeZoom = gun.attachment.id(AttachmentType.SCOPE)
            ?.let { AttachmentDefinition.from(it) }
            ?.scopeZoom(gun.attachment.scopeMode(AttachmentType.SCOPE))

        if (scopeZoom != null) {
            val currentZoom = gun.attachment.getZoom(AttachmentType.SCOPE) ?: scopeZoom.default
            val nextZoom = gun.attachment.cycleZoom(AttachmentType.SCOPE, scroll)
            if (nextZoom != null && nextZoom != currentZoom) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
            }
        } else {
            val minZoom = gun.minZoom() - 1.25
            val maxZoom = gun.maxZoom() - 1.25
            val customZoom = data.getDouble("CustomZoom")
            data.putDouble("CustomZoom", Mth.clamp(customZoom + 0.5 * scroll, minZoom, maxZoom))

            if (customZoom > minZoom && customZoom < maxZoom) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
            }
        }

        gun.invalidateProperties()
        gun.save()
    }
}
