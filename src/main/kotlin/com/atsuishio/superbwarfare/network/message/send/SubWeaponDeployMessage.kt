package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.SubWeaponDeployedMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import kotlinx.serialization.Serializable

/**
 * 主/副武器切换请求
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
