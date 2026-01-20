package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.Perk

class BackpackLinkedMagazine : Perk("backpack_linked_magazine", Type.FUNCTIONAL) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.magazine = 0
        rawData.heatPerShoot += (20 - data.perk.getLevel(this)) * 0.15
        return super.computeProperties(data, rawData)
    }
}
