package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.perk.damage.InvisibleBladeShapeness.maxCharge
import com.atsuishio.superbwarfare.perk.damage.InvisibleBladeShapeness.rate

/**
 * 隐之刃凛：轻击伤害打折，折掉的那部分存进武器上的蓄力槽；重击命中时一次性打出去。
 *
 * - 折扣率 / 转化率：1 级 50%，每级 +2.5%（两者共用 [rate]，上限 100%）；
 * - 蓄力槽上限：1 级 1000，每级 +200（[maxCharge]）。
 *
 * 数值存在 perk 自己的 NBT 条目里（`Charge`），和枪械其它状态一起随物品 NBT 同步，
 * 所以 HUD 直接用同一份数据；伤害的加减则在 `MeleeAttackMessage` 里按"轻击 / 重击"结算，
 * 不走 `modifyProperty` —— 那个会把重击也一起削了。
 */
object InvisibleBladeShapeness : Perk("invisible_blade_shapeness", Type.DAMAGE) {

    /** 蓄力值在 perk 条目里的 NBT 键 */
    private const val CHARGE_KEY: String = "Charge"

    /** 轻击伤害折扣比例，同时也是折掉部分转化为蓄力的比例（1 级 50%，每级 +2.5%） */
    fun rate(level: Int): Double = (0.5 + 0.025 * (level - 1)).coerceIn(0.0, 1.0)

    /** 蓄力槽上限（1 级 1000，每级 +200） */
    fun maxCharge(level: Int): Double = 1000.0 + 200.0 * (level - 1)

    /** 这把枪上的该 perk 实例；没装返回 `null` */
    fun instanceOf(data: GunData): PerkInstance? =
        data.perk.getInstances(Type.DAMAGE).firstOrNull { it.perk == this }

    /**
     * 当前蓄力值；**没装这个 perk 时返回 `null`**
     * （HUD 靠它区分"显示蓄力"和"显示 ∞"）。
     */
    fun chargeOrNull(data: GunData): Double? =
        instanceOf(data)?.let { data.perk.getTag(this)?.getDouble(CHARGE_KEY) ?: 0.0 }

    /** 当前蓄力值，没装该 perk 时当作 0 */
    fun charge(data: GunData): Double = chargeOrNull(data) ?: 0.0

    /** 直接写入蓄力值，自动夹在 `[0, 上限]`；没装该 perk 时什么也不做 */
    fun setCharge(data: GunData, value: Double) {
        val instance = instanceOf(data) ?: return
        val tag = data.perk.getTag(this) ?: return
        tag.putDouble(CHARGE_KEY, value.coerceIn(0.0, maxCharge(instance.level.toInt())))
    }

    /** 累加蓄力值（夹在上限内），返回累加后的值；没装该 perk 时返回 0 */
    fun addCharge(data: GunData, amount: Double): Double {
        val instance = instanceOf(data) ?: return 0.0
        val tag = data.perk.getTag(this) ?: return 0.0
        val value = (tag.getDouble(CHARGE_KEY) + amount)
            .coerceIn(0.0, maxCharge(instance.level.toInt()))
        tag.putDouble(CHARGE_KEY, value)
        return value
    }
}
