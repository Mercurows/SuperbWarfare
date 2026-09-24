package com.atsuishio.superbwarfare.client.gun

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.item.ItemStack
import java.util.*

/**
 * 动作类型。任一动作占用期间，其它入口全部拒绝（§9.5）。
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

    /** 副武器：占用时长 = `Cooldown` 或副武器数据的 RPM 周期（三期） */
    SUB_WEAPON,
}

/**
 * `GunActionLock` —— 动作互斥层（§9.5）。
 *
 * 现状是"每类动作各自零散门禁"（`reloading()`/`charging()`/`bolt.actionTimer`），
 * **开火与近战之间是空的**：`fireCooldown` 是帧驱动的 Double、`gunMelee` 是纯 tick 的全局单例，
 * 于是"换弹时挥砍"、"挥砍时开火"这类边界都能穿过去。
 *
 * 这里补上一层统一的占用标记：进入任一动作前检查 [GunAction.NONE]，
 * 占用期间其它入口**不产生任何副作用**（副作用只在真正进入动作后发生）。
 *
 * **为什么是「客户端按枪隔离」而不是全局字段**：
 * - 旧实现 `gunMelee` 是全局单例且切枪不重置 → 切枪会拿新枪数据误触发一次攻击（缺陷 1）；
 * - 状态也不能放进 `GunState`：`GunState` 全部字段服务端权威，客户端只能 `updateLocal`，
 *   任何一次服务端同步都会冲掉客户端计数（§5.2）。
 *
 * 所以状态挂在**客户端本地、按枪身份（UUID）隔离**的 [WeakHashMap] 上：切枪天然互不影响，
 * 同一把枪的连招下标也能跟着走。
 */
object GunActionLock {

    /**
     * 一把枪在客户端本地的动作状态。
     *
     * 不放进 [com.atsuishio.superbwarfare.data.gun.GunData]，也不写 NBT。
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

        /** 锁存的连招下标（挥击开始时锁存，§5.2） */
        var meleeActionIndex: Int = 0

        /** 本段动作的持续 tick（动画按它拉伸） */
        var meleeDuration: Int = 0

        /** 距离上一次近战结束过了多少 tick（用于 `MeleeComboReset` 窗口判定） */
        var sinceLastMelee: Int = 0

        /** 本次挥击是否由 G 键触发（用于 debug 与后续副武器语义） */
        var lastSwingFromSubWeaponKey: Boolean = false

        /** 当前是否被 [action] 占用 */
        fun isBusy(action: GunAction = activeAction): Boolean =
            activeAction != GunAction.NONE && (action == GunAction.NONE || activeAction == action)

        /** 是否有任何动作正在占用 */
        val isLocked: Boolean get() = activeAction != GunAction.NONE

        /**
         * [action] 这个入口现在是否该被别的动作挡住。
         *
         * **同一个动作不算阻塞自己**：连发/连挥期间会反复 acquire 同一个动作，
         * 若把"自己"也算成占用，第二次就会永远进不来。
         */
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

        /**
         * 无条件占用 [action] 并把计时**重置**成 [ticks]。
         *
         * 用于"高频重复进入的同一个动作"（连发的射击周期）：用 [acquire] 的话
         * 第二个射击周期会被自己的剩余占用挡住。
         */
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
            lastSwingFromSubWeaponKey = false
        }

        /**
         * 每 tick 递减。
         *
         * 归零时回 [GunAction.NONE]；近战结束的那一 tick 把 [sinceLastMelee] 归零开始计时。
         */
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

    /**
     * 切枪时清理**全局**层面的痕迹。
     *
     * 旧实现的 `gunMelee` 是全局单例、切枪不重置，会在切枪后拿新枪的数据误触发一次攻击。
     * 现在状态按枪隔离，切枪不需要搬运任何东西——这里只负责把"上一把枪留下的动画时间轴"停掉。
     */
    @JvmStatic
    fun onGunSwitched() {
        // 状态本身按枪隔离，无需迁移；保留这个入口是为了让调用点语义清晰，
        // 将来若有全局的动画/音效收尾需求也落在这里。
    }

    /** 仅供调试 / 重载资源时清空 */
    @JvmStatic
    fun clearAll() {
        states.clear()
        uuidStates.clear()
    }
}
