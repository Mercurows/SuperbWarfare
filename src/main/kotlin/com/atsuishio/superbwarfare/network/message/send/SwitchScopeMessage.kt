package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable

/**
 * 切换瞄具分划 / 变倍档位（滚轮）。
 *
 * ⚠ **只对主武器生效**（四期）：副武器不具有配件 —— `SubWeaponItem.canEditAttachments` 是 `false`，
 * `GunData.availableAttachments` 对副武器返回空表，`/sbw attachment` 也只认主手那把枪。
 * 所以这里刻意用**物理上的主手**（`player.mainHandItem`）而不是 `ActiveGun`：
 * 副武器被 G 切出来时它跟着一起被 `ActiveGun` 顶替的话，
 * "滚轮切瞄具"就会去操作一把根本没有瞄具槽的枪 —— 而且下一次它回到主武器时，
 * 主武器的瞄具档位一点都没变（玩家会以为滚轮失灵）。
 *
 * 客户端侧（`ClickEventHandler.onMouseScrolling`）用的是同一个判据，两边一致。
 */
@RegisterPacket
@Serializable
data class SwitchScopeMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        // 主手那把枪 —— 配件永远只挂在它上面
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

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
        SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
    }
}