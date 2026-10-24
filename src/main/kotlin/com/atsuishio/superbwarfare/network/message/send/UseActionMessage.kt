package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.event.ChargeWindowHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player

/** 右键行为里需要服务端权威的那部分；收窗口（`active = false`）时带上客户端折算好的冷却长度 */
@RegisterPacket
@Serializable
data class UseActionMessage(
    val action: RightClickAction,
    val active: Boolean = true,
    val cooldownTicks: Int = 0
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (!player.isAlive) return

        // 收窗口跟手里拿着什么无关：换枪取消时手上已经不是那把枪了，所以冷却长度由客户端带过来
        if (action == RightClickAction.BAYONET_CHARGE && !active) {
            ChargeWindowHandler.clear(player)
            if (cooldownTicks > 0) {
                player.persistentData.putInt(CHARGE_COOLDOWN, player.tickCount + cooldownTicks)
            }
            return
        }

        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        val data = GunData.from(stack)
        // 冲锋的入口不止右键（疾跑单击冲锋键），不吃"必须等于本枪的右键行为"这条
        if (action != RightClickAction.BAYONET_CHARGE && data.get(GunProp.RIGHT_CLICK_ACTION) != action) return

        if (action == RightClickAction.BAYONET_CHARGE) {
            val charge = data.get(GunProp.BAYONET_CHARGE)
            if (!charge.canCharge(data)) return
            if (onChargeCooldown(player, charge.cooldown)) return

            charge.sound?.let {
                player.level().playSound(
                    null, player.onPos,
                    SoundEvent.createFixedRangeEvent(it.location, CHARGE_SOUND_RANGE),
                    SoundSource.PLAYERS, CHARGE_SOUND_VOLUME, 1f
                )
            }
            ChargeWindowHandler.start(player, charge)
        }
    }

    companion object {
        // 冲锋冷却记在玩家的 persistentData 上，不用原版物品冷却（那会连带锁住整把枪的使用）
        private const val CHARGE_COOLDOWN = "BayonetChargeCooldown"
        private const val CHARGE_SOUND_RANGE = 32f
        private const val CHARGE_SOUND_VOLUME = 4f

        @JvmStatic
        fun clearChargeCooldown(player: Player) {
            player.persistentData.remove(CHARGE_COOLDOWN)
        }

        // 存的是"冷却结束时的 tickCount"而不是剩余量，服务端不用每 tick 递减；
        // tickCount 不落盘、重登/重生会归零，所以剩余比冷却本身还长只可能是上一局的残留，放行
        private fun onChargeCooldown(player: Player, cooldown: Int): Boolean {
            return player.persistentData.getInt(CHARGE_COOLDOWN) - player.tickCount in 1..cooldown
        }
    }
}
