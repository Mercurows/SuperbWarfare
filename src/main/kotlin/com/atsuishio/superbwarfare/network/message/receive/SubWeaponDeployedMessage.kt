package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import kotlinx.serialization.Serializable

/**
 * 主/副武器切换
 *
 * @param slot 切换后的 `ActiveSlot`（空串 = 主武器）
 * @param owner 宿主枪 UUID（仅用于日志对账）
 * @param ok 服务端是否接受了这次切换
 */
@RegisterPacket
@Serializable
data class SubWeaponDeployedMessage(
    val slot: String,
    val owner: String? = null,
    val ok: Boolean = true,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return

        debug {
            "deployed=$slot owner=$owner ok=$ok " +
                    "(client thought: ${SubWeaponClientHandler.lastRequestedSlot ?: "main"})"
        }

        if (!ok) {
            // 服务端拒绝：客户端什么都不做（不演动画、不动状态机），只给一声反馈
            SubWeaponClientHandler.onDeployRejected(player)
            return
        }

        SubWeaponClientHandler.onDeployed(player, slot)
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
