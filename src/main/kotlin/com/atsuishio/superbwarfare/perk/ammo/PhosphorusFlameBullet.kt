package com.atsuishio.superbwarfare.perk.ammo

import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.perk.AmmoPerk

object PhosphorusFlameBullet : AmmoPerk(
    Builder("phosphorus_flame_bullet", Type.AMMO)
        .bypassArmorRate(0.0).damageRate(0.8).speedRate(0.9).rgb(0xB1, 0xC1, 0xF2)
        .mobEffect(ModMobEffects.PHOSPHORUS_FIRE).hideParticle()
)