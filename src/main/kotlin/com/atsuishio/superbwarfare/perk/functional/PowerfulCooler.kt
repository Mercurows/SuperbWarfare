package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk

class PowerfulCooler : Perk("powerful_cooler", Type.FUNCTIONAL) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData? {
        rawData.naturalCooldown *= 1 + 0.05 * data.perk.getLevel(this)
        rawData.heatPerShoot *= 1 - 0.02 * data.perk.getLevel(this)
        return super.computeProperties(data, rawData)
    }
}
