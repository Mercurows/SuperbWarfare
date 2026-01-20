package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk

object HEBullet : AmmoPerk(
    Builder("he_bullet", Type.AMMO).bypassArmorRate(-0.3).damageRate(0.5).speedRate(0.85).slug().rgb(240, 20, 10)
) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.explosionDamage = (0.9 * rawData.damage * 2) * (1 + 0.1 * data.perk.getLevel(this))
        rawData.explosionRadius = (1.7 + 0.3 * data.perk.getLevel(this))
        return super.computeProperties(data, rawData)
    }
}
