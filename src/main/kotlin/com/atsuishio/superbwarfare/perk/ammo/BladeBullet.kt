package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.PerkInstance

object BladeBullet : AmmoPerk(
    Builder("blade_bullet", Type.AMMO).damageRate(0.6).speedRate(0.8).rgb(0xB4, 0x4B, 0x88)
        .mobEffect(ModMobEffects.TRAUMA)
) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        with(GunProp) {
            modifier[BYPASSES_ARMOR] -= 0.0.coerceAtLeast(1 - 0.05 * (modifier.data.perk.getLevel(this@BladeBullet) - 1))
        }
    }

    override fun getEffectAmplifier(instance: PerkInstance): Int {
        return instance.level / 2
    }
}
