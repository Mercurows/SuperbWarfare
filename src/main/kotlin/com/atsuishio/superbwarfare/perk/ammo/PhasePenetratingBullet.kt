package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.AmmoPerk

object PhasePenetratingBullet : AmmoPerk(
    Builder("phase_penetrating_bullet", Type.AMMO).speedRate(1.1).rgb(255, 255, 255)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[DAMAGE] *= (0.2 + 0.04 * modifier.data.perk.getLevel(this@PhasePenetratingBullet))
            modifier[PROJECTILE_PENETRATING] = true
        }
    }
}
