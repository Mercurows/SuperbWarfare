package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.perk.AmmoPerk

object APBullet : AmmoPerk(
    Builder("ap_bullet", Type.AMMO).bypassArmorRate(0.4).damageRate(0.9).speedRate(1.2).slug().rgb(230, 70, 35)
) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.bypassesArmor += 0.0.coerceAtLeast(0.05 * (data.perk.getLevel(this) - 1))
        return super.computeProperties(data, rawData)
    }
}
