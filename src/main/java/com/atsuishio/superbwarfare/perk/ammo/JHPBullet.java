package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class JHPBullet extends AmmoPerk {

    public JHPBullet() {
        super(new AmmoPerk.Builder("jhp_bullet", Perk.Type.AMMO).bypassArmorRate(-0.2f).damageRate(1.1f).speedRate(0.95f).slug().rgb(230, 131, 65));
    }

}
