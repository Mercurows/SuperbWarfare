package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object BackpackLinkedMagazine : Perk("backpack_linked_magazine", Type.FUNCTIONAL) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.magazine = 0
        rawData.heatPerShoot += (20 - data.perk.getLevel(this)) * 0.15
        return super.computeProperties(data, rawData)
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        modifier[MAGAZINE] = 0
        modifier[HEAT_PER_SHOOT] += (20 - modifier.data.perk.getLevel(this@BackpackLinkedMagazine)) * 0.15
    }
}
