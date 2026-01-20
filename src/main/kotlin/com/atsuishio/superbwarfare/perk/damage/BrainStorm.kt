package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk

class BrainStorm : Perk("brain_storm", Type.DAMAGE) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.headshot += 0.25 * data.perk.getLevel(this)
        return super.computeProperties(data, rawData)
    }
}
