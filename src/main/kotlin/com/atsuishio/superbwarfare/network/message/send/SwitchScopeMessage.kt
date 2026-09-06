package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
@RegisterPacket
data class SwitchScopeMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val data = from(stack)
        val scopeId = data.attachment.id(AttachmentType.SCOPE)
        val definition = scopeId?.let { AttachmentDefinition.from(it) }
        if (definition?.supportsScopeSwitching() == true) {
            data.attachment.cycleScopeMode(AttachmentType.SCOPE, scroll)
        } else {
            val tag = data.tag()
            tag.putBoolean("ScopeAlt", !tag.getBoolean("ScopeAlt"))
        }
        data.save()
    }
}