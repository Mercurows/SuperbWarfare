package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentLightState
import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/**
 * 手电/激光开关按键（客户端 → 服务端）。
 *
 * - [ACTION_CYCLE_ALL]（短按）：所有发光配件各自切到下一档；
 * - [ACTION_TURN_OFF_ALL]（长按）：所有发光配件全部关闭。
 *
 * 档位存在每件配件自己的槽位 NBT（`Attachments.<Slot>.LightMode`）里。
 * [hotbarSlot] / [gunUuid] 用于防止网络延迟/切枪窗口把操作落到另一把枪上。
 */
@RegisterPacket
@Serializable
data class ToggleFlashlightMessage(
    val action: Int,
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
        if (AttachmentDefinition.findFlashlights(gun).isEmpty() &&
            AttachmentDefinition.findLaserEmitters(gun).isEmpty()
        ) return

        when (action) {
            ACTION_CYCLE_ALL -> AttachmentLightState.cycleAll(gun)
            ACTION_TURN_OFF_ALL -> AttachmentLightState.turnOffAll(gun)
        }
    }

    companion object {
        const val ACTION_CYCLE_ALL: Int = 0
        const val ACTION_TURN_OFF_ALL: Int = 1
    }
}
