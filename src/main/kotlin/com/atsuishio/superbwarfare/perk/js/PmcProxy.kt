package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.Prop
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp

class PmcProxy(private val pmc: PMC<GunData, DefaultGunData>) {
    private fun findGunProp(key: String): Prop<*, *, *, *, *> {
        return GunProp.entries.firstOrNull { it.serializationName == key }
            ?: throw IllegalArgumentException("Unknown GunProp serializationName: '$key'")
    }

    private fun shouldIgnoreMagazineModifier(key: String): Boolean {
        return key == "Magazine" && pmc.data.getDefault().magazine.list.size > 1
    }

    /**
     * Coerce a Number value to the correct type for the given property.
     * JS numbers always arrive as Double, but Int props need Int values.
     */
    private fun coerceValue(prop: Prop<*, *, *, *, *>, value: Any?): Any? {
        if (value !is Number) return value
        return when (val current = pmc.getUnchecked(prop)) {
            is Int -> value.toInt()
            is Long -> value.toLong()
            is Float -> value.toFloat()
            is Double -> value.toDouble()
            is Short -> value.toShort()
            is Byte -> value.toByte()
            else -> value.toDouble() // default to Double for unknown number types
        }
    }

    fun get(key: String): Any? = pmc.getUnchecked(findGunProp(key))

    fun set(key: String, value: Any?) {
        if (shouldIgnoreMagazineModifier(key)) return
        val prop = findGunProp(key)
        pmc.setUnchecked(prop, coerceValue(prop, value))
    }

    fun add(key: String, amount: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        if (shouldIgnoreMagazineModifier(key)) return current
        val result = current + amount.toDouble()
        val coerced = coerceValue(prop, result)
        pmc.setUnchecked(prop, coerced)
        return coerced as Number
    }

    fun mul(key: String, factor: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        if (shouldIgnoreMagazineModifier(key)) return current
        val result = current * factor.toDouble()
        val coerced = coerceValue(prop, result)
        pmc.setUnchecked(prop, coerced)
        return coerced as Number
    }

    fun clampMin(key: String, min: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        if (shouldIgnoreMagazineModifier(key)) return current
        val result = maxOf(current, min.toDouble())
        val coerced = coerceValue(prop, result)
        pmc.setUnchecked(prop, coerced)
        return coerced as Number
    }

    fun clampMax(key: String, max: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        if (shouldIgnoreMagazineModifier(key)) return current
        val result = minOf(current, max.toDouble())
        val coerced = coerceValue(prop, result)
        pmc.setUnchecked(prop, coerced)
        return coerced as Number
    }

    fun isShotgun(): Boolean = pmc[GunProp.PROJECTILE_AMOUNT] > 1
}
