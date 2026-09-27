package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.SubWeaponDeployedMessage
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable

/**
 * 副武器「主/副武器切换」请求报文（客户端 → 服务端）。
 *
 * 报文只表达**意图**：`这次 G 请把当前操控的枪切成谁`。
 * **成不成交给服务端一个人拍板**（与三期 §11.8.1-⑫ 给开火定的口径同源）：
 * 客户端虽然也能从同步过来的 `GunState.ActiveSlot` 看出当前状态，但那份永远慢一拍，
 * 两边各写一次就会出现"谁都不动"的永久死角。
 * 所以四期把"服务端拍板"的对象从"这一发打没打出去"换成"**这次切没切成**"。
 *
 * 客户端**收到 [SubWeaponDeployedMessage] 确认才演切换动作**。
 *
 * @param slot 要部署的副武器槽位（`AttachmentType` 枚举名，如 `"SUBWEAPON"`）；
 *   `null` / 空串 = 切回主武器
 * @param clientActive 客户端认为当前是什么（**仅用于日志对账**，服务端不据此决策）
 */
@RegisterPacket
@Serializable
data class SubWeaponDeployMessage(
    val slot: String? = null,
    val clientActive: String? = null,
) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator) return

        // ⚠ 这里看的是**真·主手**：整套部署状态就挂在主手那把枪的 NBT 上（§9.8.10）
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) {
            debug { "deploy rejected: main hand is not an operable gun" }
            player.sendPacket(SubWeaponDeployedMessage("", null, ok = false))
            return
        }

        val gun = GunData.from(stack)

        if (slot.isNullOrEmpty()) {
            ActiveGun.holster(gun)
            player.sendPacket(SubWeaponDeployedMessage("", gun.uuid?.toString(), ok = true))
            return
        }

        val type = AttachmentType.entries.firstOrNull { it.name == slot }
        if (type == null) {
            debug { "deploy rejected: unknown slot '$slot'" }
            player.sendPacket(SubWeaponDeployedMessage("", gun.uuid?.toString(), ok = false))
            return
        }

        val instance = SubWeaponRuntime.find(gun, type, client = false)
        if (instance == null) {
            debug { "'$slot' is not an installed sub-weapon on ${gun.id}" }
            player.sendPacket(SubWeaponDeployedMessage("", gun.uuid?.toString(), ok = false))
            return
        }

        ActiveGun.deploy(gun, type, instance)
        player.sendPacket(SubWeaponDeployedMessage(type.name, gun.uuid?.toString(), ok = true))
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
