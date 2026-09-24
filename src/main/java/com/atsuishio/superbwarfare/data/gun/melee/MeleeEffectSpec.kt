package com.atsuishio.superbwarfare.data.gun.melee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战额外效果的配置项（`MeleeAction.Effects` 里的一项）。
 *
 * 一期只做**数据解析 + DataValidator 校验**，行为注册表（`ModMeleeEffects`）与结算在后续阶段落地。
 *
 * ```jsonc
 * "Effects": [
 *   "superbwarfare:heavy_impact",
 *   { "Effect": "superbwarfare:warhead_stab", "Chance": 0.5 },
 *   { "Type": "superbwarfare:explosion", "Chance": 0.25, "Radius": 4.0, "Damage": 60 }
 * ]
 * ```
 */
@Serializable
data class MeleeEffectSpec(
    /** 预设 id（`sbw/melee_effects` 下的 json）；与 [type] 都有则预设 + 覆盖 */
    @SerialName("Effect")
    val effect: String? = null,

    /** 行为 id；与 [effect] 都有则预设 + 覆盖 */
    @SerialName("Type")
    val type: String? = null,

    /** 触发概率（只在服务端 roll） */
    @SerialName("Chance")
    val chance: Double = 1.0,

    /** 触发时机 */
    @SerialName("Trigger")
    val trigger: MeleeEffectTrigger = MeleeEffectTrigger.HIT,

    /** 该效果自己的冷却（写进枪械 NBT 冷却表） */
    @SerialName("Cooldown")
    val cooldown: Int = 0,

    // 以下字段由各行为自行解释
    @SerialName("Damage")
    val damage: Double? = null,

    @SerialName("Radius")
    val radius: Double? = null,

    @SerialName("DestroyBlocks")
    val destroyBlocks: Boolean? = null,

    @SerialName("FireTime")
    val fireTime: Int? = null,

    @SerialName("Knockback")
    val knockback: Double? = null,

    @SerialName("Lift")
    val lift: Double? = null,

    @SerialName("Duration")
    val duration: Int? = null,

    @SerialName("Amplifier")
    val amplifier: Int? = null,

    @SerialName("Count")
    val count: Int? = null,

    @SerialName("Sound")
    val sound: String? = null,

    @SerialName("Particle")
    val particle: String? = null,

    /** 留给未来 */
    @SerialName("Extra")
    val extra: String? = null,
)

/** 近战额外效果的触发时机 */
@Serializable
enum class MeleeEffectTrigger {
    /** 每个命中目标各 roll 一次 */
    @SerialName("Hit")
    HIT,

    /** 每次挥击只 roll 一次（第一个命中目标） */
    @SerialName("FirstHit")
    FIRST_HIT,

    /** 无论是否命中都 roll 一次（挥击瞬间） */
    @SerialName("Swing")
    SWING,

    /** 击杀时 roll */
    @SerialName("Kill")
    KILL,
}
