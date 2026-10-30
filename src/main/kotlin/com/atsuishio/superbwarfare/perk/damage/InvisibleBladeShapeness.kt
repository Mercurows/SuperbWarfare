package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu.Companion.MAX_PERK_LEVEL
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance


object InvisibleBladeShapeness : Perk("invisible_blade_shapeness", Type.DAMAGE) {

    private const val CHARGE_KEY: String = "Charge"

    fun rate(level: Int): Double {
        val steps = (MAX_PERK_LEVEL - 1).coerceAtLeast(1)
        return 0.5 + 0.5 * (level - 1).coerceIn(0, steps).toDouble() / steps
    }

    fun maxCharge(level: Int): Double = 1200.0 + 200.0 * (level - 1)

    fun instanceOf(data: GunData): PerkInstance? =
        data.perk.getInstances(Type.DAMAGE).firstOrNull { it.perk == this }

    fun chargeOrNull(data: GunData): Double? =
        instanceOf(data)?.let { data.perk.getTag(this)?.getDouble(CHARGE_KEY) ?: 0.0 }

    fun charge(data: GunData): Double = chargeOrNull(data) ?: 0.0

    fun setCharge(data: GunData, value: Double) {
        val instance = instanceOf(data) ?: return
        val tag = data.perk.getTag(this) ?: return
        tag.putDouble(CHARGE_KEY, value.coerceIn(0.0, maxCharge(instance.level.toInt())))
    }

    fun addCharge(data: GunData, amount: Double): Double {
        val instance = instanceOf(data) ?: return 0.0
        val tag = data.perk.getTag(this) ?: return 0.0
        val value = (tag.getDouble(CHARGE_KEY) + amount)
            .coerceIn(0.0, maxCharge(instance.level.toInt()))
        tag.putDouble(CHARGE_KEY, value)
        return value
    }
}
