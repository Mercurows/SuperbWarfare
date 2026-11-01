package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.*

/**
 * 动作类型。任一动作占用期间，其它入口全部拒绝。
 */
enum class GunAction {
    NONE,

    /** 开火：占用时长 = 一个射击周期 */
    FIRING,

    /** 换弹：占用时长 = 现有换弹计时器（由 `Reload` 状态机自己管） */
    RELOADING,

    /** 拉栓：占用时长 = 现有 `bolt.actionTimer` */
    BOLTING,

    /** 近战：占用时长 = 本段动作的 `Duration` */
    MELEE,

    /** 主/副武器切换：占用时长 = `max(两把枪的 DrawTime) + 余量` */
    SUB_WEAPON,
}

/**
 * 动作互斥层
 */
object GunActionLock {

    /**
     * 一把枪在客户端本地的动作状态
     */
    class State {
        /** 当前占用的动作 */
        var activeAction: GunAction = GunAction.NONE
            private set

        /** 剩余占用 tick */
        var actionTicks: Int = 0
            private set

        /** 当前近战段剩余 tick（0 = 没有正在进行的近战） */
        var meleeTicks: Int = 0

        /** 锁存的连招下标 */
        var meleeActionIndex: Int = 0

        /** 本段动作的持续 tick（动画按它拉伸） */
        var meleeDuration: Int = 0

        /** 本段的结算点：`meleeTicks` 递减到它就打一次（= `Duration - HitTime`），**从大到小**排列 */
        var meleeHitTicks: IntArray = IntArray(0)

        /** 已经结算到第几个结算点；等于 [meleeHitTicks] 长度时本段不再出伤 */
        var meleeHitIndex: Int = 0

        /** 本段挥击是不是刺刀冲锋打出来的（决定用 `Animation.BayonetChargeHit` 换掉本段 clip） */
        var meleeFromBayonetCharge: Boolean = false

        /** 右键蓄势中（抬起保持）：不占动作锁，但其它近战输入要让位 */
        var meleeHolding: Boolean = false

        /** 蓄势已经按住的 tick 数（松开时和 `HoldDuration` 比） */
        var meleeHoldTicks: Int = 0

        /** 本段挥击是蓄势松开打出来的（决定用 `MeleeAction.ReleaseAnimation` 换掉本段 clip） */
        var meleeHeavyRelease: Boolean = false

        /** 冲锋撞上的目标：本段无论如何都要打中它；结算后清掉 */
        var meleeChargeTarget: Entity? = null

        // UUID 在复活后不变，挥击必须绑定发起它的玩家实体和世界实例。
        private var meleeOwner: Player? = null
        private var meleeLevel: Level? = null

        fun bindMeleeOwner(player: Player) {
            meleeOwner = player
            meleeLevel = player.level()
        }

        /** 右键蓄势：抬起保持中 */
        fun beginHeavyHold(player: Player) {
            meleeHolding = true
            meleeHoldTicks = 0
            meleeHeavyRelease = false
            bindMeleeOwner(player)
        }

        fun endHeavyHold() {
            meleeHolding = false
            meleeHoldTicks = 0
        }

        fun isMeleeValidFor(player: Player): Boolean =
            meleeOwner === player && meleeLevel === player.level() &&
                    player.isAlive && !player.isRemoved && !player.isSpectator

        /** 距离上一次近战结束过了多少 tick（用于 `MeleeComboReset` 窗口判定） */
        var sinceLastMelee: Int = Int.MAX_VALUE

        /** 当前是否被 [action] 占用 */
        fun isBusy(action: GunAction = activeAction): Boolean =
            activeAction != GunAction.NONE && (action == GunAction.NONE || activeAction == action)

        /** 是否有任何动作正在占用 */
        val isLocked: Boolean get() = activeAction != GunAction.NONE

        fun blocks(action: GunAction): Boolean =
            activeAction != GunAction.NONE && activeAction != action

        // 收尾窗口内且结算点都已打完才允许提前起下一段
        fun canChainMelee(window: Int): Boolean =
            activeAction == GunAction.MELEE &&
                    meleeTicks in 1..window &&
                    meleeHitIndex >= meleeHitTicks.size

        // 只拆当前这一段，连招下标与窗口留给下一段
        fun releaseMeleeForChain() {
            if (activeAction != GunAction.MELEE) return
            activeAction = GunAction.NONE
            actionTicks = 0
            meleeTicks = 0
            meleeHitTicks = IntArray(0)
            meleeHitIndex = 0
            meleeFromBayonetCharge = false
            meleeHeavyRelease = false
            meleeHoldTicks = 0
            meleeChargeTarget = null
            meleeOwner = null
            meleeLevel = null
            sinceLastMelee = 0
        }

        /** 尝试占用 [action]，成功返回 true；已被任何动作占用时返回 false 且不产生副作用 */
        fun acquire(action: GunAction, ticks: Int): Boolean {
            if (isLocked) return false
            activeAction = action
            actionTicks = ticks.coerceAtLeast(1)
            return true
        }

        /** 释放占用（只释放 [action]，避免误清掉别人的占用） */
        fun release(action: GunAction) {
            if (activeAction == action) {
                activeAction = GunAction.NONE
                actionTicks = 0
            }
        }

        fun force(action: GunAction, ticks: Int) {
            activeAction = action
            actionTicks = ticks.coerceAtLeast(1)
        }

        fun clear() {
            activeAction = GunAction.NONE
            actionTicks = 0
            meleeHolding = false
            meleeHoldTicks = 0
            cancelMelee()
        }

        /** 切走武器时取消挥击和连招，不影响换弹/拉栓等其它动作锁。 */
        fun cancelMelee() {
            release(GunAction.MELEE)
            meleeHolding = false
            meleeHoldTicks = 0
            meleeTicks = 0
            meleeActionIndex = 0
            meleeDuration = 0
            meleeHitTicks = IntArray(0)
            meleeHitIndex = 0
            meleeFromBayonetCharge = false
            meleeHeavyRelease = false
            meleeChargeTarget = null
            meleeOwner = null
            meleeLevel = null
            sinceLastMelee = Int.MAX_VALUE
        }

        fun tick() {
            val owner = meleeOwner
            if ((meleeTicks > 0 || meleeHolding) && (owner == null || !isMeleeValidFor(owner))) {
                clear()
                return
            }
            if (meleeHolding) meleeHoldTicks++
            if (meleeTicks > 0) {
                meleeTicks--
                if (meleeTicks == 0) {
                    release(GunAction.MELEE)
                    sinceLastMelee = 0
                }
            } else if (sinceLastMelee < Int.MAX_VALUE) {
                sinceLastMelee++
            }

            if (actionTicks > 0) {
                actionTicks--
                if (actionTicks == 0) {
                    activeAction = GunAction.NONE
                }
            }
        }
    }

    private val states = WeakHashMap<GunData, State>()
    private val uuidStates = WeakHashMap<UUID, State>()
    private var heldState: State? = null

    /** 必须在推进当前主手的计时/结算前调用，空手和非枪械也要传入 null。 */
    fun updateHeld(data: GunData?) {
        val state = data?.let { of(it) }
        if (heldState !== state) {
            heldState?.cancelMelee()
            heldState = state
        }
    }

    /** 取（或新建）这把枪的本地状态 */
    @JvmStatic
    fun of(data: GunData): State {
        val uuid = data.uuid
        if (uuid != null) {
            return uuidStates.getOrPut(uuid) { State() }
        }
        return states.getOrPut(data) { State() }
    }

    @JvmStatic
    fun of(stack: ItemStack): State = of(GunData.from(stack))

    @JvmStatic
    fun clearAll() {
        heldState?.clear()
        heldState = null
        states.values.forEach(State::clear)
        uuidStates.values.forEach(State::clear)
        states.clear()
        uuidStates.clear()
    }
}
