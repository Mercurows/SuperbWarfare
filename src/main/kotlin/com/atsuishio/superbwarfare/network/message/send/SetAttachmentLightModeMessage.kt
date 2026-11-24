package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentLightState
import com.atsuishio.superbwarfare.data.attachment.FlashlightMode
import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/**
 * 改装界面里单独设置某件配件的档位（客户端 → 服务端）。
 *
 * 用 ordinal 传 [AttachmentType] / [FlashlightMode]，避免枚举序列化兼容问题。
 * 服务端会校验槽位确实装着带 `Laser`/`Flashlight` 定义的配件，以及 [hotbarSlot]/[gunUuid] 防串枪。
 */
@RegisterPacket
@Serializable
data class SetAttachmentLightModeMessage(
    val slot: Int,
    val mode: Int,
    val hotbarSlot: Int = -1,
    val gunUuid: String? = null,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (hotbarSlot >= 0 && player.inventory.selected != hotbarSlot) return

        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        val gun = stack.toGunData() ?: return
        if (gunUuid != null && gun.uuid?.toString() != gunUuid) return

        val type = AttachmentType.entries.getOrNull(slot) ?: return
        val definition = AttachmentDefinition.from(gun.attachment.id(type) ?: return) ?: return
        if (!AttachmentLightState.hasEmitter(definition)) return

        AttachmentLightState.setMode(gun, type, FlashlightMode.byOrdinal(mode))
    }
}
