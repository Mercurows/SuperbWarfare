package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.physics.tracked.TrackedTerrainDebugService
import net.minecraft.network.chat.Component

/**
 * One press from a key or a controller button bound to
 * `key.superbwarfare.toggle_bullet_terrain`.
 *
 * The server still owns the subscription. Permission 2 is required, same as
 * `/sbwtest bt show`. The player does not have to be riding: the view covers
 * every tracked vehicle already loaded in this dimension.
 */
@RegisterPacket
object ToggleBulletTerrainMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("需要权限 2 才能开关 Bullet 地形线框"), true)
            return
        }
        val message = if (TrackedTerrainDebugService.watching(player)) {
            TrackedTerrainDebugService.hide(player)
            "已关闭 Bullet 地形线框"
        } else {
            TrackedTerrainDebugService.show(player)
        }
        player.displayClientMessage(Component.literal(message), true)
    }
}
