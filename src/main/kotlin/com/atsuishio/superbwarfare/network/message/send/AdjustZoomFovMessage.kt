package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth

/**
 * 调整当前瞄具的倍率（滚轮，用于没有离散档位的变倍镜）。
 *
 * ⚠ **与 [SwitchScopeMessage] 同一条口径：只对主武器生效。**
 * 客户端 `ClickEventHandler.onMouseScrolling` 是拿**物理主手**那把枪的
 * `canSwitchScope()` / `canAdjustZoom()` 决定发哪条报文的，所以服务端也必须操作同一把枪 ——
 * 否则副武器被 G 切出来时，滚轮按主武器的能力选了"调倍率"，报文却在调**副武器**的倍率
 * （副武器没有瞄具槽，落到 `CustomZoom` 分支去写一个根本不会被读到的值），
 * 表现就是"滚轮毫无反应"（§9.8.11）。
 */
@RegisterPacket
@Serializable
data class AdjustZoomFovMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        // 主手那把枪 —— 配件永远只挂在它上面
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

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
