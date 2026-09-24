package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.StringInstanceBuilder
import com.atsuishio.superbwarfare.data.StringOrObjectFactory
import com.atsuishio.superbwarfare.data.gun.melee.MeleeAction.Companion.MeleeActionInstanceBuilder
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 近战动作表（`MeleeActions`）里的一段。
 *
 * 既可以是**字符串简写**（就是本段动画 clip 名）：
 * ```jsonc
 * "MeleeActions": [ "hit_lr", { "Animation": "hit_rl", "DamageMultiplier": 1.25 } ]
 * ```
 * 也可以是完整对象。字符串写法通过 [StringOrObjectFactory] 走 [MeleeActionInstanceBuilder]。
 *
 * **作用域**：`MeleeActions`/`MeleeHitbox`/`MeleeSweep`/`MeleeComboReset` 是 PMC 属性；
 * 本类里的字段**不参与 PMC**，读取时用 `?:` 从全局字段继承（字段级缺省继承）。
 */
@StringOrObjectFactory(MeleeActionInstanceBuilder::class)
@Serializable
data class MeleeAction(
    /**
     * 本段动画 clip 名的**候选链**；不写时按 `GunAnimation.Melee[idx % size]` 解析。
     *
     * - 写字符串 = 单候选（`"hit"` 或 `"animation.ak_47.hit"`）；
     * - 写列表 = 按顺序取**第一个存在**的 clip。
     *
     * 短名会被拼成 `animation.<宿主枪 id>.<短名>`（见 `GunAnimationNames`）——动作表所在的枪械数据
     * 会被多把枪共用（配件/弹种覆盖），写不了某把枪的完整名字，拼接让"有专属动画就用专属的、
     * 没有就退回通用的"能用一条数据表达。
     */
    @SerialName("Animation")
    val animation: SingleOrList<String>? = null,

    /** 本段总 tick（动画按它拉伸）；不写时用 `MeleeDuration` */
    @SerialName("Duration")
    val duration: Int? = null,

    /** **从挥击开始算，第几 tick 结算**；不写时用 `MeleeDamageTime` */
    @SerialName("HitTime")
    val hitTime: Int? = null,

    /** 本段判定形状；不写时用全局 `MeleeHitbox` */
    @SerialName("Hitbox")
    val hitbox: MeleeHitbox? = null,

    /** 本段横扫；不写时用全局 `MeleeSweep` */
    @SerialName("Sweep")
    val sweep: MeleeSweep? = null,

    /** 本段伤害（与 [damageMultiplier] 二选一，写了 [damage] 就不再乘倍率） */
    @SerialName("Damage")
    val damage: Double? = null,

    /** 本段伤害倍率，乘在 `MeleeDamage` 上 */
    @SerialName("DamageMultiplier")
    val damageMultiplier: Double? = null,

    /** 最多命中几个目标；`<= 0` 表示不限 */
    @SerialName("MaxTargets")
    val maxTargets: Int? = null,

    /** 排序后的伤害衰减系数：第 i 个目标乘 `max(1 - i * Falloff, 0.1)` */
    @SerialName("Falloff")
    val falloff: Double? = null,

    /** 排序方式 */
    @SerialName("SortBy")
    val sortBy: MeleeSortBy? = null,

    /** 击退强度；0 = 不击退 */
    @SerialName("Knockback")
    val knockback: Double? = null,

    /** 穿甲：走 `DamageHandler` 那条强制伤害路径的伤害占比 */
    @SerialName("BypassesArmor")
    val bypassesArmor: Double? = null,

    /** 本段打头倍率；不写时用枪的 `Headshot` */
    @SerialName("Headshot")
    val headshot: Double? = null,

    /** 本段打腿倍率；不写时用投射物默认的 0.5 */
    @SerialName("Legshot")
    val legshot: Double? = null,

    /** 本段消耗的枪械耐久 */
    @SerialName("Durability")
    val durability: Int? = null,

    /** 本段冷却（写进枪械 NBT 冷却表） */
    @SerialName("Cooldown")
    val cooldown: Int? = null,

    /** 本段挥击音效；不写时用枪的 `MeleeSound.Swing` */
    @SerialName("Swing")
    val swing: SerializedSoundEvent? = null,

    /** 本段命中音效；不写时用枪的 `MeleeSound.Hit` */
    @SerialName("Hit")
    val hit: SerializedSoundEvent? = null,

    /** 本段额外效果（后续阶段启用，本期只做解析与校验） */
    @SerialName("Effects")
    val effects: List<MeleeEffectSpec>? = null,
) : IDBasedData<MeleeAction> {

    @kotlinx.serialization.Transient
    var itemId: String = ""

    override fun getId() = itemId

    override fun setId(id: String) {
        this.itemId = id
    }

    /** 本段声明的动画候选链（短名或全名）；没写时为空列表，调用方回退到 `GunAnimation.Melee` */
    fun animationCandidates(): List<String> = animation?.list.orEmpty()

    /** 本段实际使用的判定形状（字段级缺省继承） */
    fun hitboxOr(global: MeleeHitbox?) = hitbox ?: global

    /** 本段实际使用的横扫（字段级缺省继承） */
    fun sweepOr(global: MeleeSweep?) = sweep ?: global

    /** 本段实际使用的排序方式 */
    fun sortByOr() = sortBy ?: MeleeSortBy.ANGLE

    /** 本段实际使用的衰减系数 */
    fun falloffOr() = falloff ?: DEFAULT_FALLOFF

    /**
     * 把本段配置解析成一份**完全展开**的判定参数。
     *
     * 所有 `?:` 继承都在这里一次性做完，判定工具 [com.atsuishio.superbwarfare.tools.MeleeQuery]
     * 与调试渲染拿到的是同一个不含 null 的结构。
     */
    fun resolve(
        defaultDuration: Int,
        defaultHitTime: Int,
        defaultDamage: Double,
        defaultAngle: Double,
        /** 枪的 `MeleeRange`：叠加在形状自己的 `Range` 之上的额外距离 */
        defaultRange: Double,
        defaultSweep: MeleeSweep?,
        defaultHitbox: MeleeHitbox?,
    ): ResolvedMeleeAction {
        val resolvedHitbox = hitbox ?: defaultHitbox ?: MeleeHitbox()
        val resolvedDuration = (duration ?: defaultDuration).coerceAtLeast(1)
        return ResolvedMeleeAction(
            hitbox = resolvedHitbox.copy(
                // `MeleeRange` 是**叠加**在形状自己的 Range 之上的额外距离，配件要加近战距离就加它。
                // 形状没写 Range 时基数取 0，于是退化成"距离由 MeleeRange 决定"，与旧数据行为一致。
                range = resolvedHitbox.rangeOr(0.0) + defaultRange,
                angle = resolvedHitbox.angleOr(defaultAngle),
            ),
            sweep = sweep ?: defaultSweep,
            duration = resolvedDuration,
            // 与旧的 `GunProp` 全局钳制（`MeleeDamageTime <= MeleeDuration - 1`）同一个道理：
            // 结算 tick 落在挥击之外就永远不会出伤，所以夹进本段时长内。
            hitTime = (hitTime ?: defaultHitTime).coerceIn(0, resolvedDuration),
            damage = damage ?: (defaultDamage * (damageMultiplier ?: 1.0)),
            maxTargets = maxTargets ?: 0,
            falloff = falloffOr(),
            sortBy = sortByOr(),
            knockback = knockback ?: 0.0,
            bypassesArmor = bypassesArmor ?: 0.0,
            headshot = headshot,
            legshot = legshot,
            durability = durability ?: 0,
            cooldown = cooldown ?: 0,
            swing = swing,
            hit = hit,
        )
    }

    companion object {
        /** 默认伤害衰减：第 i 个目标乘 `max(1 - i * 0.1, 0.1)` */
        const val DEFAULT_FALLOFF: Double = 0.1

        /** 打腿倍率的默认值，与投射物保持一致 */
        const val DEFAULT_LEGSHOT: Double = 0.5

        /** `"hit_lr"` 这样的字符串简写 = 只写动画名（单个候选）的动作 */
        object MeleeActionInstanceBuilder : StringInstanceBuilder<MeleeAction> {
            override fun fromString(value: String): MeleeAction {
                val trimmed = value.trim()
                if (trimmed.isEmpty()) {
                    Mod.LOGGER.warn("Empty melee action string, falling back to a bare MeleeAction")
                    return MeleeAction()
                }
                return MeleeAction(animation = SingleOrList(trimmed))
            }
        }
    }
}
