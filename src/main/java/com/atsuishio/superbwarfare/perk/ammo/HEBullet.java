package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class HEBullet extends AmmoPerk {

    public HEBullet() {
        super(new AmmoPerk.Builder("he_bullet", Perk.Type.AMMO).bypassArmorRate(-0.3f).damageRate(0.5f).speedRate(0.85f).slug().rgb(240, 20, 10));
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.explosionDamage = (0.9 * rawData.damage * 2) * (1 + 0.1 * gunData.perk.getLevel(this));
        rawData.explosionRadius = (1.7 + 0.3 * gunData.perk.getLevel(this));
        return super.computeProperties(gunData, rawData);
    }
}
