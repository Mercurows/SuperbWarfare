package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.Perk

object PowerfulCooler : Perk("powerful_cooler", Type.FUNCTIONAL) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[NATURAL_COOLDOWN] *= (1 + 0.05 * modifier.data.perk.getLevel(this@PowerfulCooler))
            modifier[HEAT_PER_SHOOT] *= (1 - 0.02 * modifier.data.perk.getLevel(this@PowerfulCooler))
        }
    }
}
