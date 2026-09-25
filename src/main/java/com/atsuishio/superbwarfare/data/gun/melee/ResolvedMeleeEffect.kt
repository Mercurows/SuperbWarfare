package com.atsuishio.superbwarfare.data.gun.melee

/**
 * [MeleeEffectSpec.resolve] 的输出：一份**完全展开、不含歧义**的效果参数。
 *
 * 判定"要不要触发"所需的一切都在这里（[chance] / [trigger] / [cooldown]），
 * 各行为再从剩下的字段里各取所需（见 `MeleeEffectBehaviors`）。
 *
 * @param key 冷却键与调试名。取 `Effect`（预设 id），没写预设时取 `Type`（行为 id）
 * @param type 行为 id（已归一化，不带命名空间），用于查 `ModMeleeEffects`
 * @param chance 触发概率 `[0, 1]`；**只在服务端 roll**
 * @param trigger 触发时机
 * @param cooldown 触发成功后的冷却 tick（0 = 不冷却）
 */
data class ResolvedMeleeEffect(
    val key: String,
    val type: String,
    val chance: Double,
    val trigger: MeleeEffectTrigger,
    val cooldown: Int,

    val damage: Double?,
    val radius: Double?,
    val destroyBlocks: Boolean?,
    val fireTime: Int?,
    val knockback: Double?,
    val lift: Double?,
    val duration: Int?,
    val amplifier: Int?,
    val count: Int?,
    val sound: String?,
    val particle: String?,
    val extra: String?,
    val amplitude: Double?,
) {
    /** 给日志/命令用的一行摘要 */
    fun describe(): String = buildString {
        append(type).append("(key=").append(key)
        append(" chance=").append(chance)
        append(" trigger=").append(trigger)
        if (cooldown > 0) append(" cooldown=").append(cooldown)
        append(')')
    }
}
