package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.client.gun.GunChargeHandler.reset
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.RightClickAction
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.UseActionMessage
import com.atsuishio.superbwarfare.tools.MeleeQuery
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToInt

/** 刺刀冲锋的客户端状态 */
object GunChargeHandler {

    /** 窗口剩余 tick；最后一 tick 的攻击由调用方确认后再结束冲锋。 */
    var chargeTicks: Int = 0
        private set

    // 冷却剩余 tick；记在玩家身上（和手里拿什么无关，换枪也不清），不用原版物品冷却
    var cooldownTicks: Int = 0
        private set

    // 窗口总长（tick）与满格冷却总长（tick）；换枪之后都留着，HUD 拿它们算条子的比例
    var chargeTotal: Int = 0
        private set
    var cooldownTotal: Int = 0
        private set

    private var chargingItem: Item? = null

    // 冲锋刺击打 `MeleeActions` 的第几段（`ClientEventHandler` 的 `requestedIndex` 用同一个）
    const val STRIKE_ACTION_INDEX = 0

    // 冷却的最低量：不管窗口消耗了多少，冷却至少按消耗一半算（走得短的红条从一半处开始恢复）
    private const val MIN_SPENT = 0.5f

    // 未触发攻击而耗尽窗口时，从空条恢复，但速度是普通冷却的两倍。
    private const val EXPIRED_RECOVERY_SPEED = 2f

    fun start(stack: ItemStack, data: GunData) {
        if (isCharging() || cooldownTicks > 0) return
        if ((stack.item as? GunItem)?.isBroken(stack) == true) return

        val charge = data.get(GunProp.BAYONET_CHARGE)
        if (!charge.canCharge(data)) return

        chargeTicks = charge.duration
        chargeTotal = charge.duration
        cooldownTotal = charge.cooldown
        chargingItem = stack.item

        sendPacketToServer(UseActionMessage(RightClickAction.BAYONET_CHARGE))
    }

    // 每客户端 tick 递减
    fun tickCooldown() {
        if (cooldownTicks > 0) cooldownTicks--
    }

    /**
     * 推进窗口；返回**撞上的那个目标**（`null` = 这 tick 不出手）。
     *
     * 只报告，不清窗口（到点除外），真正打出去了由调用方 [reset]。返回的实体就是这次的刺击目标，
     * 结算那一下会强行把它算进命中里（见 `MeleeQuery.resolve` 的 `forced`）。
     *
     * 判据与结算侧同一个（[MeleeQuery.isChargeTarget]）：戳不了的东西不消耗这一击，
     * 窗口继续跑，免得白打一下空。
     */
    fun tick(player: Player, stack: ItemStack): Entity? {
        if (!isCharging()) return null

        // 换枪就作废（窗口跟着上一把枪走）
        if (!GunItem.isHeldWeapon(stack) || stack.item !== chargingItem) {
            reset()
            return null
        }

        // 最后一 tick 找到目标但未能出手时，在下一 tick 正常按超时收窗口。
        if (chargeTicks <= 0) {
            reset(expired = true)
            return null
        }

        chargeTicks--

        // 撞到东西才打；窗口内可以一直等。
        val reach = GunData.from(stack).resolveMeleeAction(STRIKE_ACTION_INDEX).hitbox.range
        val looking = TraceTool.findLookingEntity(player, reach)
        val target = looking?.takeIf { MeleeQuery.isChargeTarget(player, it) }

        // 最后一 tick 仍允许攻击；未找到目标则直接进入加速恢复。
        if (chargeTicks <= 0 && target == null) reset(expired = true)

        return target
    }

    // 手里换成别的东西（空手/别的物品）时窗口也得收
    fun cancelIfItemChanged(stack: ItemStack) {
        if (isCharging() && stack.item !== chargingItem) reset()
    }

    fun isCharging(): Boolean = chargingItem != null

    // 收窗口（打完那一击、换枪、切状态、到点）；冷却从这一刻才开始
    fun reset(expired: Boolean = false) {
        val charging = isCharging()
        val spent = if (charging) 1f - chargeTicks.toFloat() / chargeTotal.coerceAtLeast(1) else 0f
        chargeTicks = 0
        chargingItem = null
        if (charging) {
            if (expired) {
                // 分母和剩余量一起减半，让 HUD 从 0 起恢复，而不是从半条开始。
                cooldownTotal = (cooldownTotal / EXPIRED_RECOVERY_SPEED).roundToInt()
                cooldownTicks = cooldownTotal
            } else {
                cooldownTicks = scaledCooldown(spent)
            }
            sendPacketToServer(
                UseActionMessage(
                    RightClickAction.BAYONET_CHARGE,
                    active = false,
                    cooldownTicks = cooldownTicks
                )
            )
        }
    }

    // 冷却长度按窗口里实际消耗掉的比例折算，最少折算 MIN_SPENT：消耗 80% 就冷却 80%，只消耗 20% 也冷却 50%
    private fun scaledCooldown(spent: Float): Int =
        (cooldownTotal * spent.coerceIn(MIN_SPENT, 1f)).roundToInt()

    fun clear() {
        chargeTicks = 0
        chargeTotal = 0
        cooldownTicks = 0
        cooldownTotal = 0
        chargingItem = null
    }
}
