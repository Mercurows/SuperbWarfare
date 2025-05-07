package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;

public class BladeBullet extends AmmoPerk {

    public BladeBullet() {
        super(new AmmoPerk.Builder("blade_bullet", Perk.Type.AMMO)
//                .bypassArmorRate(-0.2f).damageRate(0.7f).speedRate(0.8f).rgb(0xB4, 0x4B, 0x88).mobEffect(() -> CompatHolder.DMV_BLEEDING)
        );
    }

    @Override
    public int getEffectAmplifier(PerkInstance instance) {
        return instance.level() / 3;
    }
}
