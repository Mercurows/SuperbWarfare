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

    /** 从挥击开始算，第几 tick 结算 */
    val hitTime: Int,

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

    /** 打头倍率覆盖；null = 用枪的 `Headshot` */
    val headshot: Double?,

    /** 打腿倍率覆盖；null = 用 [MeleeAction.DEFAULT_LEGSHOT] */
    val legshot: Double?,

    /** 本段消耗的耐久 */
    val durability: Int,

    /** 本段冷却 */
    val cooldown: Int,

    /** 本段挥击音效覆盖；null = 用枪的 `MeleeSound.Swing` */
    val swing: SerializedSoundEvent?,

    /** 本段命中音效覆盖；null = 用枪的 `MeleeSound.Hit` */
    val hit: SerializedSoundEvent?,
) {
    /**
     * 本段实际生效的结算下标，**从挥击开始算第几 tick**。
     *
     * 就是 `duration - hitTime`：`meleeTicks` 从 [duration] 每 tick 递减，
     * 递减 [hitTime] 次之后正好等于 `duration - hitTime`（见 `MeleeClientHandler.tickActiveSwing`）。
     *
     * 收敛到 `[0, duration]`，不会越界。
     */
    val hitTickFromStart: Int get() = (duration - hitTime).coerceIn(0, duration)

    fun resolvedHeadshot(gunHeadshot: Double) = headshot ?: gunHeadshot

    fun resolvedLegshot() = legshot ?: MeleeAction.DEFAULT_LEGSHOT
}
