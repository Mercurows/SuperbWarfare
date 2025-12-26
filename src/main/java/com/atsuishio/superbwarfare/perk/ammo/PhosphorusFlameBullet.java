package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class PhosphorusFlameBullet extends AmmoPerk {
    public PhosphorusFlameBullet() {
        super(new AmmoPerk.Builder("phosphorus_flame_bullet", Perk.Type.AMMO)
                .bypassArmorRate(0.0f).damageRate(0.8f).speedRate(0.9f).rgb(0xB1, 0xC1, 0xF2).mobEffect(ModMobEffects.PHOSPHORUS_FIRE).hideParticle());
    }
}