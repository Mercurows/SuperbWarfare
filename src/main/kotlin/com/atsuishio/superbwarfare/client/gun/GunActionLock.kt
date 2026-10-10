package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.item.ItemStack
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

        /** 本段近战是否已经结算过（每个动作只结算一次） */
        var meleeHitResolved: Boolean = false

        /** 锁存的连招下标 */
        var meleeActionIndex: Int = 0

        /** 本段动作的持续 tick（动画按它拉伸） */
        var meleeDuration: Int = 0

        /** 距离上一次近战结束过了多少 tick（用于 `MeleeComboReset` 窗口判定） */
        var sinceLastMelee: Int = 0

        /** 当前是否被 [action] 占用 */
        fun isBusy(action: GunAction = activeAction): Boolean =
            activeAction != GunAction.NONE && (action == GunAction.NONE || activeAction == action)

        /** 是否有任何动作正在占用 */
        val isLocked: Boolean get() = activeAction != GunAction.NONE

        fun blocks(action: GunAction): Boolean =
            activeAction != GunAction.NONE && activeAction != action

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
            meleeTicks = 0
            meleeHitResolved = false
            meleeActionIndex = 0
            meleeDuration = 0
            sinceLastMelee = 0
        }

        fun tick() {
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
        states.clear()
        uuidStates.clear()
    }
}
