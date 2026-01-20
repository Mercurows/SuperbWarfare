package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk

object HighImpactReserves : Perk("high_impact_reserves", Type.DAMAGE) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        val rate = data.ammo.get().toDouble() / 1.coerceAtLeast(rawData.magazine)
        val level = data.perk.getLevel(this)
        val limit = 0.5 + (level - 1) * 0.02

        if (rate <= limit) {
            val min1 = 0.12
            val max1 = 0.25

            val min20 = 0.75
            val max20 = 1.5

            val t = (level - 1) / 19.0

            val minOutput = min1 + t * (min20 - min1)
            val maxOutput = max1 + t * (max20 - max1)

            rawData.damage *= (1 + (1 - (rate / limit)) * (maxOutput - minOutput) + minOutput)
        }
        return super.computeProperties(data, rawData)
    }
}
