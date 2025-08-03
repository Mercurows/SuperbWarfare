package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class APBullet extends AmmoPerk {

    public APBullet() {
        super(new AmmoPerk.Builder("ap_bullet", Perk.Type.AMMO).bypassArmorRate(0.4).damageRate(0.9).speedRate(1.2).slug().rgb(230, 70, 35));
        appendModification(GunProp.BYPASSES_ARMOR, (data, v) -> v + Math.max(0, 0.05 * (data.perk.getLevel(this) - 1)));
    }
}
