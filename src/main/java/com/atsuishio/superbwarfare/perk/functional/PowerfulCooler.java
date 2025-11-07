package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;

public class PowerfulCooler extends Perk {

    public PowerfulCooler() {
        super("powerful_cooler", Perk.Type.FUNCTIONAL);
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.naturalCooldown *= 1 + 0.05 * gunData.perk.getLevel(this);
        rawData.heatPerShoot *= 1 - 0.02 * gunData.perk.getLevel(this);
        return super.computeProperties(gunData, rawData);
    }
}
