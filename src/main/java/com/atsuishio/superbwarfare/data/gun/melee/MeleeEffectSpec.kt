package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.data.StringOrObjectFactory
import com.atsuishio.superbwarfare.init.ModMeleeEffects
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战额外效果（`MeleeAction.Effects` 里的一项）。
 *
 * 一个条目既可以**引用预设**（`sbw/melee_effects/<id>.json`），也可以**直接写行为**，
 * 还可以两者并存 —— 预设提供行为与参数，条目上的非空字段逐项覆盖它。
 *
 * ```jsonc
 * "Effects": [
 *   "superbwarfare:heavy_impact",                                  // 字符串简写 = 引用预设
 *   { "Effect": "superbwarfare:warhead_stab", "Chance": 0.35 },     // 预设 + 覆盖概率
 *   { "Type": "superbwarfare:explosion", "Chance": 0.25, "Radius": 4.0, "Damage": 60 }
 * ]
 * ```
 *
 * **所有字段都可空**：本类同时充当"预设文件的数据类"（`sbw/melee_effects` 下的 json），
 * 而预设要能表达"这一项我没写"才能让条目上的值正确覆盖它。
 * 真正的默认值在 [resolve] 里一次性补上（与 `MeleeAction.resolve` 同一套路）。
 *
 * 概率、冷却与触发时机的落地规则：
 * - [chance] **只在服务端** roll（`level.random`），客户端不参与；
 * - [cooldown] 写进枪械 NBT 冷却表（键 `effect:<id>`），**只有真的触发了才写**；
 * - [trigger] 决定这一项在一次挥击里被考虑几次（见 [MeleeEffectTrigger]）。
 */
@StringOrObjectFactory(MeleeEffectSpec.MeleeEffectSpecInstanceBuilder::class)
@Serializable
data class MeleeEffectSpec(
    /** 预设 id（`sbw/melee_effects` 下的 json）；与 [type] 都有则预设 + 覆盖 */
    @SerialName("Effect")
    val effect: String? = null,

    /** 行为 id（`ModMeleeEffects` 注册的那个）；不写时取预设的，预设也没有时取 [effect] 本身 */
    @SerialName("Type")
    val type: String? = null,

    /** 触发概率 `[0, 1]`；不写时取预设的，预设也没有时为 1.0 */
    @SerialName("Chance")
    val chance: Double? = null,

    /** 触发时机；不写时取预设的，预设也没有时为 `Hit` */
    @SerialName("Trigger")
    val trigger: MeleeEffectTrigger? = null,

    /** 该效果自己的冷却 tick（写进枪械 NBT 冷却表）；不写时取预设的，预设也没有时为 0 */
    @SerialName("Cooldown")
    val cooldown: Int? = null,

    // ---- 以下字段由各行为自行解释 ----

    /** `explosion` 伤害 / `extra_damage` 附加伤害 / `heal` 治疗量 */
    @SerialName("Damage")
    val damage: Double? = null,

    /** `explosion` 半径 / `screen_shake` 传送半径 / `sound` 传播半径倍率 / `particle` 扩散 */
    @SerialName("Radius")
    val radius: Double? = null,

    /** `explosion` 是否破坏方块（默认 **false**：近战触发的爆炸不该拆家） */
    @SerialName("DestroyBlocks")
    val destroyBlocks: Boolean? = null,

    /** `ignite` 点燃 tick / `explosion` 引燃时间 */
    @SerialName("FireTime")
    val fireTime: Int? = null,

    /** `knockback` 水平击退强度 */
    @SerialName("Knockback")
    val knockback: Double? = null,

    /** `knockback` 上挑（垂直分量）—— 仓库没有现成原语，由行为自己写 y 分量 */
    @SerialName("Lift")
    val lift: Double? = null,

    /** `shock`/`potion` 效果时长 / `screen_shake` 持续时间（tick） */
    @SerialName("Duration")
    val duration: Int? = null,

    /** `shock`/`potion` 效果等级 */
    @SerialName("Amplifier")
    val amplifier: Int? = null,

    /** `ammo_refund` 返还发数 / `particle` 粒子数量 */
    @SerialName("Count")
    val count: Int? = null,

    /** `sound` 行为播放的音效 id */
    @SerialName("Sound")
    val sound: String? = null,

    /** `particle` 行为生成的粒子 id */
    @SerialName("Particle")
    val particle: String? = null,

    /**
     * 行为自己的补充参数。
     *
     * 目前用于 `potion`：`Extra` 就是 `MobEffect` 的注册 id（如 `minecraft:slowness`）。
     */
    @SerialName("Extra")
    val extra: String? = null,

    /** `screen_shake` 的震动幅度：把"时间 / 半径 / 幅度"三者分开，各行为按需解释 */
    @SerialName("Amplitude")
    val amplitude: Double? = null,
) {

    /**
     * 把「本条目 + 它引用的预设」展开成一份完全确定的参数。
     *
     * 覆盖规则是**逐字段**的：条目上写了就用条目的，没写就用预设的，预设也没有就用行为默认值。
     *
     * @param preset 已解析的预设；传 `null` 表示"不用预设"，不传时按 [effect] 自行查表
     * @return 解析结果；既没有可用行为也找不到预设时返回 `null`（调用方自行决定是否打日志）
     */
    @JvmOverloads
    fun resolve(preset: MeleeEffectSpec? = effect?.let { CustomData.MELEE_EFFECTS[it] }): ResolvedMeleeEffect? {
        val behaviorId = ModMeleeEffects.normalize(type)
            ?: ModMeleeEffects.normalize(preset?.type)
            // 预设找不到、Type 也没写时，把 Effect 本身当成行为 id：
            // 于是 `"Effects": ["superbwarfare:explosion"]` 这种写法也能直接用
            ?: ModMeleeEffects.normalize(effect)?.takeIf { ModMeleeEffects.get(it) != null }
            ?: return null

        return ResolvedMeleeEffect(
            key = effect ?: type ?: behaviorId,
            type = behaviorId,
            chance = (chance ?: preset?.chance ?: 1.0).coerceIn(0.0, 1.0),
            trigger = trigger ?: preset?.trigger ?: MeleeEffectTrigger.HIT,
            cooldown = (cooldown ?: preset?.cooldown ?: 0).coerceAtLeast(0),
            damage = damage ?: preset?.damage,
            radius = radius ?: preset?.radius,
            destroyBlocks = destroyBlocks ?: preset?.destroyBlocks,
            fireTime = fireTime ?: preset?.fireTime,
            knockback = knockback ?: preset?.knockback,
            lift = lift ?: preset?.lift,
            duration = duration ?: preset?.duration,
            amplifier = amplifier ?: preset?.amplifier,
            count = count ?: preset?.count,
            sound = sound ?: preset?.sound,
            particle = particle ?: preset?.particle,
            extra = extra ?: preset?.extra,
            amplitude = amplitude ?: preset?.amplitude,
        )
    }

    /** `"superbwarfare:heavy_impact"` 这样的字符串简写 = 只引用预设 */
    object MeleeEffectSpecInstanceBuilder : StringInstanceBuilder<MeleeEffectSpec> {
        override fun fromString(value: String): MeleeEffectSpec {
            val trimmed = value.trim()
            // 与对象写法保持同一套推断：能查到预设就是预设，查不到就是行为 id。
            // 这里不做查表（字符串转换发生在数据加载期间，预设表可能还没填好），
            // 两者都填上即可 —— [resolve] 会按"预设优先、行为兜底"的顺序解析。
            return MeleeEffectSpec(effect = trimmed, type = trimmed)
        }
    }
}

/** 近战额外效果的触发时机 */
@Serializable
enum class MeleeEffectTrigger {
    /** 每个真正受伤的目标各考虑一次 */
    @SerialName("Hit")
    HIT,

    /** 每次挥击只考虑一次（第一个通过命中判定的目标） */
    @SerialName("FirstHit")
    FIRST_HIT,

    /** 无论是否命中都考虑一次（挥击瞬间） */
    @SerialName("Swing")
    SWING,

    /** 目标被这一击打死时各考虑一次 */
    @SerialName("Kill")
    KILL,
}
