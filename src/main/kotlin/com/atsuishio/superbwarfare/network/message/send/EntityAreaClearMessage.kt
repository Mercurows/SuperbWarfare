package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.item.weapon.BeastItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.AABB

@Serializable
data class EntityAreaClearMessage(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (!player.hasPermissions(2)) return
        player.server.execute {
            val level = player.level()
            val aabb = AABB(minX, minY, minZ, maxX, maxY, maxZ)
            val targets = level.getEntities(null, aabb)
            for (target in targets) {
                if (target == player) continue
                BeastItem.beastKill(player, target)
            }
        }
    }
}
