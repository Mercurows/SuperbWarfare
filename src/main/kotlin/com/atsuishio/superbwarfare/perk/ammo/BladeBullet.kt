package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance

class BladeBullet : AmmoPerk(
    Builder("blade_bullet", Type.AMMO).damageRate(0.6).speedRate(0.8).rgb(0xB4, 0x4B, 0x88)
        .mobEffect(ModMobEffects.TRAUMA)
) {
    override fun computeProperties(
        data: GunData,
        rawData: DefaultGunData
    ): DefaultGunData {
        rawData.bypassesArmor -= 0.0.coerceAtLeast(1 - 0.05 * (data.perk.getLevel(this) - 1))
        return super.computeProperties(data, rawData)
    }

    override fun getEffectAmplifier(instance: PerkInstance): Int {
        return instance.level / 2
    }
}
