package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object BackpackLinkedMagazine : Perk("backpack_linked_magazine", Type.FUNCTIONAL) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[MAGAZINE] = 0
            modifier[HEAT_PER_SHOOT] += (20 - modifier.data.perk.getLevel(this@BackpackLinkedMagazine)) * 0.15
        }
    }
}
