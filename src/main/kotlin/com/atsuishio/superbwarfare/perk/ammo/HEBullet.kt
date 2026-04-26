package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.perk.AmmoPerk

object HEBullet : AmmoPerk(
    Builder("he_bullet", Type.AMMO).bypassArmorRate(-0.3).damageRate(0.5).speedRate(0.85).slug().rgb(240, 20, 10)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[EXPLOSION_DAMAGE] =
                (0.9 * modifier[DAMAGE] * 2) * (1 + 0.1 * modifier.data.perk.getLevel(this@HEBullet))
            modifier[EXPLOSION_RADIUS] = (1.7 + 0.3 * modifier.data.perk.getLevel(this@HEBullet))
        }
    }
}
