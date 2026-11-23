package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.FlashlightState
import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

@RegisterPacket
object ToggleFlashlightMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        val gun = stack.toGunData() ?: return
        if (AttachmentDefinition.findFlashlights(gun).isEmpty()) return

        FlashlightState.cycle(gun)
    }
}
