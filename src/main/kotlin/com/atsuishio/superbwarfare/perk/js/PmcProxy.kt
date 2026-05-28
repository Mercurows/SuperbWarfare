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

    fun get(key: String): Any? = pmc.getUnchecked(findGunProp(key))

    fun set(key: String, value: Any?) = pmc.setUnchecked(findGunProp(key), value)

    fun add(key: String, amount: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        val result = current + amount.toDouble()
        pmc.setUnchecked(prop, result)
        return result
    }

    fun mul(key: String, factor: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        val result = current * factor.toDouble()
        pmc.setUnchecked(prop, result)
        return result
    }

    fun clampMin(key: String, min: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        val result = maxOf(current, min.toDouble())
        pmc.setUnchecked(prop, result)
        return result
    }

    fun clampMax(key: String, max: Number): Number {
        val prop = findGunProp(key)
        val current = (pmc.getUnchecked(prop) as Number).toDouble()
        val result = minOf(current, max.toDouble())
        pmc.setUnchecked(prop, result)
        return result
    }

    fun isShotgun(): Boolean = pmc.data.isShotgun
}
