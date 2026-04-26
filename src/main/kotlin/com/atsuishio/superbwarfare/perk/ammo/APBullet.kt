package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.AmmoPerk

object APBullet : AmmoPerk(
    Builder("ap_bullet", Type.AMMO).bypassArmorRate(0.4).damageRate(0.9).speedRate(1.2).slug().rgb(230, 70, 35)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[BYPASSES_ARMOR] += 0.0.coerceAtLeast(0.05 * (modifier.data.perk.getLevel(this@APBullet) - 1))
        }
    }
}
