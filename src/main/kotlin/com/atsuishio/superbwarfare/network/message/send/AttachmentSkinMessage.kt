package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/** 只提交皮肤；身份校验与配件调整一致，不覆盖尚未确认的角度、偏移或颜色。 */
@RegisterPacket
@Serializable
data class AttachmentSkinMessage(
    val gunId: String,
    val gunUuid: String?,
    val hotbarSlot: Int,
    val slot: String,
    val attachmentId: String,
    val skinId: String,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.inventory.selected != hotbarSlot) return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val type = AttachmentType.entries.firstOrNull { it.attachmentName == slot } ?: return
        val data = GunData.from(stack)
        if (data.getDefault().itemId != gunId || data.uuid?.toString() != gunUuid ||
            data.attachment.id(type)?.toString() != attachmentId) return
        val definition = data.attachment.id(type)?.let(AttachmentDefinition::from) ?: return
        val reset = skinId == AttachmentDefinition.DEFAULT_SKIN_ID
        if (!reset && definition.skinOrNull(skinId) == null) return
        data.attachment.setSkin(type, if (reset) null else skinId)
        data.save()
    }
}
