package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;

public class BladeBullet extends AmmoPerk {

    public BladeBullet() {
        super(new AmmoPerk.Builder("blade_bullet", Perk.Type.AMMO).damageRate(0.6f).speedRate(0.8f).rgb(0xB4, 0x4B, 0x88).mobEffect(() -> ModMobEffects.TRAUMA));
        appendModification(GunProp.BYPASSES_ARMOR, (data, v) -> v - Math.max(0, 1 - 0.05 * (data.perk.getLevel(this) - 1)));
    }

    @Override
    public int getEffectAmplifier(PerkInstance instance) {
        return instance.level() / 2;
    }
}
