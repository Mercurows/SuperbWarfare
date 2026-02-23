package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.GunEventHandler.playGunBoltSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * 开火按键按下/松开时的处理
 */
@Serializable
data class FireKeyMessage(val type: Int, val power: Double, val zoom: Boolean) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator) return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val data = from(stack)

        if (type == 0) {
            // 按下开火
            data.item.onFireKeyPress(data, player, zoom)
        } else if (type == 1) {
            // 松开开火
            data.item.onFireKeyRelease(data, player, power, zoom)
            queueServerWork(4) { handleGunBolt(player, stack) }
        }

        data.save()
    }

    private fun handleGunBolt(player: Player, stack: ItemStack) {
        if (stack.item !is GunItem) return
        val data = from(stack)

        if (data.get(GunProp.BOLT_ACTION_TIME) > 0
            && data.hasEnoughAmmoToShoot(player)
            && data.bolt.actionTimer.get() == 0
            && !data.reloading()
            && !data.charging()
        ) {
            if (!player.cooldowns.isOnCooldown(stack.item) && data.bolt.needed.get()) {
                data.startBolt()
                playGunBoltSounds(player, data)
            }
        }
    }
}
