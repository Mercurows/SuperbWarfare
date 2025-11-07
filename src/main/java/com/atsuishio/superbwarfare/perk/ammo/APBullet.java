package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class APBullet extends AmmoPerk {

    public APBullet() {
        super(new AmmoPerk.Builder("ap_bullet", Perk.Type.AMMO).bypassArmorRate(0.4).damageRate(0.9).speedRate(1.2).slug().rgb(230, 70, 35));
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.bypassesArmor += Math.max(0, 0.05 * (gunData.perk.getLevel(this) - 1));
        return super.computeProperties(gunData, rawData);
    }
}
