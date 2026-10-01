package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.JsonOverrideApplier
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.data.PropertyModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * 充能射击档位：开火时**额外消耗枪械自身内置电池的 FE**，并用 [override] 覆写这一发的属性。
 *
 * 与 [ChargeInfo] 的区别：那个是「开火模式」的蓄力时长/威力机制（`chargePower` 乘 Damage/Velocity），
 * 纯时间驱动、不碰能量；本类是完全独立的另一套，由电量与开镜状态驱动。
 *
 * ```json
 * "ChargeAction": [
 *   { "OneShotCost": 3000, "OnlyZooming": true, "Override": { "Damage": 160, "Velocity": 20 } }
 * ]
 * ```
 *
 * 一次开火时按数组顺序取**第一个条件满足**的档位（见 `GunData.chargeActionFor`）：
 * [onlyZooming] 为真时要求本次开镜，且枪械当前 FE 必须够 [oneShotCost]。
 * 都不满足就退回普通射击，不扣电也不覆写。数组顺序即优先级，作者可自行排序。
 *
 * [override] 的键就是 `DefaultGunData` 的 JSON 字段名（交由 [JsonOverrideApplier] 按
 * `GunProp.entries` 的 `serializationName` 解析），因此 `SoundInfo` 这类整块对象也能直接换掉。
 *
 * ⚠ **写错键名是静默的**（[JsonOverrideApplier] 查不到就 `continue`），所以加键时先确认它是个
 * 真的 `GunProp`。
 *
 * 服务端只读这套属性里的射击数值（伤害/爆炸/音效…），而其中 [GunProp.SHOOT_ANIMATION] 是
 * **客户端**读的：动画那一步（`ClientEventHandler.onClientGunFire`）发生在开火窗口内部，
 * 靠的正是上面 `setChargeAction` 挂上的瞬时档位。
 */
@Serializable
data class ChargeAction(
    /** 触发一次充能射击所消耗的 FE。 */
    @SerialName("OneShotCost")
    val oneShotCost: Int = 1000,

    /** 是否只在开镜状态下才允许触发。 */
    @SerialName("OnlyZooming")
    val onlyZooming: Boolean = false,

    /** 触发时对本次射击覆写的属性，键为 `DefaultGunData` 的 JSON 字段名。 */
    @SerialName("Override")
    val override: JsonObject? = null,
) : PropertyModifier<GunData, DefaultGunData> {

    /**
     * 实际扣除的 FE。
     *
     * 夹到非负：负数交给 `extractEnergy` 会被当成「请求 0」而静默不扣，白白送出一发强化弹。
     */
    val cost: Int
        get() = oneShotCost.coerceAtLeast(0)

    /**
     * [override] 里 [prop] 这一项的**字面数值**；没写、或写的不是数字时返回 `null`。
     *
     * 这条旁路只给**逐帧**读取用（目前唯一的消费者是散布 `ZoomSpreadRate`，
     * 见 `GunData.zoomSpreadRateFor`）：属性流水线是按发的瞬态状态
     * （[modifyProperty] 只在开火那一发的调用栈里生效），而散布要在**瞄准期间逐帧**算
     * ——为了一个数字每帧开两次流水线（配件/弹种/perk 全链）不值当，所以直接读原始值。
     *
     * 只认字面量数字：写对象/数组/字符串一律当作"没写"。那些属性必须走流水线，
     * 才能拿到类型转换与上下限缩限，本方法不做也不该做这些。
     */
    fun numericOverride(prop: Prop<GunData, DefaultGunData, *, *, *>): Double? =
        (override?.get(prop.serializationName) as? JsonPrimitive)?.doubleOrNull

    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonOverrideApplier(GunProp.entries)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        jsonPropModifier.update(override)
        jsonPropModifier.modifyProperty(modifier)
    }
}
