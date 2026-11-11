package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.GunModificationCode
import com.atsuishio.superbwarfare.data.attachment.GunModificationException
import com.atsuishio.superbwarfare.data.attachment.GunModificationService
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.GunModificationResultMessage
import com.atsuishio.superbwarfare.tools.playLocalSound
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class GunModificationApplyMessage(
    val requestId: String,
    val code: String,
    val expectedCode: String,
    val gunUuid: String?,
    val hotbarSlot: Int,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (requestId.length > 64) return
        try {
            if (code.length > GunModificationCode.MAX_INPUT_LENGTH || expectedCode.length > GunModificationCode.MAX_CODE_LENGTH)
                throw GunModificationException("invalid")
            val stack = player.mainHandItem
            if (player.inventory.selected != hotbarSlot || stack.item !is GunItem) throw GunModificationException("changed")
            val data = GunData.from(stack)
            if (data.uuid?.toString() != gunUuid || GunModificationService.fingerprint(data) != expectedCode)
                throw GunModificationException("changed")
            val preset = GunModificationCode.decode(code)
            val prepared = GunModificationService.prepare(data, preset, player)
            GunModificationService.apply(player, data, prepared)
            player.inventory.setChanged()
            player.containerMenu.broadcastChanges()
            player.playLocalSound(ModSounds.ATTACHMENT_SET.get(), 1f, 1f)
            player.sendPacket(GunModificationResultMessage(requestId, true))
        } catch (e: GunModificationException) {
            player.playLocalSound(ModSounds.ATTACHMENT_FAIL.get(), 1f, 1f)
            player.sendPacket(GunModificationResultMessage(requestId, false, e.reason, e.details.toList()))
        }
    }
}
