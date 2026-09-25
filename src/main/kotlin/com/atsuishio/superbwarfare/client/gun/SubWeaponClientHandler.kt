package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.send.SubWeaponFireMessage
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.entity.player.Player

/**
 * 客户端副武器触发（G 键）。
 *
 * 语义表：
 *
 * | 情况 | V | G |
 * |---|---|---|
 * | 什么都没装 | 主武器自身近战 | **等同 V**（本类返回 `false`，交回近战入口） |
 * | 只装刺刀（无 `SubWeapon`） | 刺刀动作 | **等同 V** |
 * | 只装副武器 | 主武器自身近战 | **使用副武器** |
 * | 刺刀 + 副武器共存 | 刺刀动作 | **使用副武器** |
 *
 * 「多个副武器」不做优先级 —— **遍历一次、逐个触发**：
 * 冷却中跳过、已经在装填/拉栓的跳过、剩下的进 [SubWeaponFireMessage.slots]；
 * 动作占用取所有实际触发者里最长的一个，避免"遍历触发"被动作锁逐个拦掉。
 *
 * **客户端不决定"开火还是装填"**：那是服务端的事（见报文的类注释）。
 * 这里只做三件事 —— 挑出这一次 G 要操作的槽位、按需给一声反馈、上动作锁给个节奏。
 * 冷却也由服务端写（写在主武器冷却表上），客户端只读 —— 单一事实来源，
 * 免得两边各写一次导致"谁都不动"的死角。
 *
 * 全部副武器都在冷却时：只播一声 `trigger_click` 反馈，不产生其它副作用
 * （这时**不会**落到近战 —— 手上明明有副武器却挥了一刀是最容易误伤的设计）。
 */
object SubWeaponClientHandler {

    /** 装填占用的兜底时长（真正的释放靠服务端状态同步，见 `MeleeClientHandler.syncServerDrivenLocks`） */
    private const val MIN_RELOAD_LOCK_TICKS = 20

    /**
     * 尝试用副武器消费这次 G。
     *
     * @return `true` = 本次 G 已经归副武器（无论成功开火/装填，还是只给了一声"没反应"的反馈）；
     *   `false` = 主武器上**没有任何副武器**，调用方应当把 G 当成 V 走近战入口。
     */
    @JvmStatic
    fun tryTrigger(player: Player, data: GunData, state: GunActionLock.State): Boolean {
        val instances = SubWeaponRuntime.installed(data)
        if (instances.isEmpty()) return false

        // 有副武器就吞掉这次 G：动作被占用 / 主武器自己在换弹拉栓时什么都不做
        if (state.isLocked) return true
        if (data.reloading() || data.charging() || data.bolt.actionTimer.get() > 0) return true

        val slots = ArrayList<String>(instances.size)
        var lockTicks = 0

        for (instance in instances) {
            val subWeapon = instance.data

            if (data.cooldown.isCoolingDown(instance.cooldownKey)) {
                debug { "subweapon ${instance.slotName} skipped: cooling down" }
                continue
            }

            // 已经在装填/拉栓：别再刷报文，等它自己走完
            if (subWeapon.reloading() || subWeapon.bolt.actionTimer.get() > 0) {
                debug { "subweapon ${instance.slotName} skipped: busy reloading/bolting" }
                continue
            }

            val canShoot = subWeapon.canShoot(player)

            // 打不出去、背包里也没有它要的弹药：给一声反馈，别让这次 G 无声无息地消失
            if (!canShoot && !subWeapon.hasBackupAmmo(player)) {
                debug {
                    "subweapon ${instance.slotName} skipped: no ammo and no backup ammo for " +
                            subWeapon.get(GunProp.PROJECTILE).itemId
                }
                lockTicks = maxOf(lockTicks, MIN_RELOAD_LOCK_TICKS)
                continue
            }

            slots += instance.slotName
            // 能开火 → 按射击周期占锁；空仓 → 按它自己的换弹时间占锁（顺带把报文频率压到一次/换弹）
            lockTicks = maxOf(
                lockTicks,
                if (canShoot) instance.cooldownTicks() else reloadLockTicks(subWeapon),
            )
        }

        if (slots.isEmpty()) {
            debug { "subweapon: nothing actionable this press (see the per-slot reasons above)" }
            // 也占一小段锁：否则按住 G 会每 tick 播一声 trigger_click（"没反应"的反馈本身也该有节奏）
            state.acquire(GunAction.SUB_WEAPON, maxOf(lockTicks, MIN_RELOAD_LOCK_TICKS))
            player.playSound(ModSounds.TRIGGER_CLICK.get(), 1f, 1f)
            return true
        }

        state.acquire(GunAction.SUB_WEAPON, lockTicks.coerceAtLeast(1))

        sendPacketToServer(
            SubWeaponFireMessage(
                slots = slots,
                spread = ClientEventHandler.gunSpread,
                zoom = ClientEventHandler.zoom,
                uuid = ClientEventHandler.lockedEntity?.uuid,
            )
        )

        debug { "subweapon trigger: slots=$slots lock=$lockTicks gun=${data.id}" }
        return true
    }

    /** 装填占用时长：取该副武器自己的空仓/常规换弹时间里更长的那个 */
    private fun reloadLockTicks(data: GunData): Int {
        val empty = data.get(GunProp.EMPTY_RELOAD_TIME)
        val normal = data.get(GunProp.NORMAL_RELOAD_TIME)
        return maxOf(empty, normal, MIN_RELOAD_LOCK_TICKS)
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[SubWeapon] {}", message())
        }
    }
}
