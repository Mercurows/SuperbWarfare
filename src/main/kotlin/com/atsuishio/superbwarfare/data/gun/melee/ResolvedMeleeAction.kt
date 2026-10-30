package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent

/**
 * [MeleeAction.resolve] 的输出：一份**完全展开、不含 null** 的判定参数。
 *
 * 判定工具（`MeleeQuery`）、客户端执行器与调试渲染都只认这个结构，
 * 字段级缺省继承（`?:`）只在 [MeleeAction.resolve] 里做一次。
 */
data class ResolvedMeleeAction(
    /** 判定形状（`range`/`angle` 已按全局字段补齐） */
    val hitbox: MeleeHitbox,

    /** 横扫；null = 静态判定 */
    val sweep: MeleeSweep?,

    /** 本段总 tick */
    val duration: Int,

    /** 从挥击开始算，第几 tick 结算；多段出伤时有多项 */
    val hitTimes: List<Int>,

    /** 本段基础伤害（已乘 `DamageMultiplier`） */
    val damage: Double,

    /** 最多命中数；`<= 0` = 不限 */
    val maxTargets: Int,

    /** 伤害衰减系数 */
    val falloff: Double,

    /** 排序方式 */
    val sortBy: MeleeSortBy,

    /** 击退强度 */
    val knockback: Double,

    /** 穿甲占比 */
    val bypassesArmor: Double,

    /** 打头倍率覆盖；null = 用枪的 `MeleeHeadshot` */
    val headshot: Double?,

    /** 打腿倍率覆盖；null = 用枪的 `MeleeLegshot` */
    val legshot: Double?,

    /** 本段每次命中消耗的耐久 */
    val durability: Int,

    /** 本段冷却 */
    val cooldown: Int,

    /** 本段挥击音效覆盖；null = 用枪的 `MeleeSound.Swing` */
    val swing: SerializedSoundEvent?,

    /** 本段命中音效覆盖；null = 用枪的 `MeleeSound.Hit` */
    val hit: SerializedSoundEvent?,

    /**
     * 本段声明的额外效果（**原始条目**，没展开预设）。
     *
     * 展开结果见 [effects] —— 那里是 `by lazy` 的：客户端拿到的
     * [ResolvedMeleeAction] 从来不用 [effects]，而预设表（`sbw/melee_effects`）
     * 只在服务端加载，客户端展开只会白打一堆"找不到预设"的日志。
     */
    val effectSpecs: List<MeleeEffectSpec> = emptyList(),

    /**
     * 本段是不是"重击"段（`MeleeAction.Heavy`）：
     * 轻击/重击的区别要参与伤害结算（例如隐之刃凛只在轻击时蓄力、只在重击时释放）。
     */
    val heavy: Boolean = false,
) {
    /**
     * 本段实际生效的额外效果（预设 + 内联覆盖已合并）。
     *
     * 第一次访问时解析并缓存；解析不出来的条目会被丢掉（数据问题由 `DataValidator` 报）。
     */
    val effects: List<ResolvedMeleeEffect> by lazy { effectSpecs.mapNotNull { it.resolve() } }

    /**
     * 本段所有结算下标（`meleeTicks` 的阈值），按 `meleeTicks` 递减方向排列。
     *
     * 每个值就是 `duration - hitTime`：`meleeTicks` 从 [duration] 每 tick 递减，
     * 越过一个阈值就结算一次。多段出伤因此按 `HitTime` 升序逐段触发。
     * 刺刀冲锋那一下会用 `BayonetCharge.Strike*` 覆写这一组量（见 `MeleeClientHandler`）。
     *
     * 收敛到 `[1, duration]`：阈值为 0 时 `meleeTicks` 已经先被清零释放，那一段永远不会结算。
     */
    val hitTicksFromStart: List<Int>
        get() = hitTimes.sorted().map { (duration - it).coerceIn(1, duration) }

    /** 本段实际生效的打头倍率：本段覆盖 ?: 枪的 `MeleeHeadshot` */
    fun resolvedHeadshot(gunHeadshot: Double) = headshot ?: gunHeadshot

    /** 本段实际生效的打腿倍率：本段覆盖 ?: 枪的 `MeleeLegshot` */
    fun resolvedLegshot(gunLegshot: Double) = legshot ?: gunLegshot
}
