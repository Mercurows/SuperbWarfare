package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;

public class BackpackLinkedMagazine extends Perk {

    public BackpackLinkedMagazine() {
        super("backpack_linked_magazine", Perk.Type.FUNCTIONAL);
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.magazine = 0;
        rawData.heatPerShoot += (20 - gunData.perk.getLevel(this)) * 0.15;

        return super.computeProperties(gunData, rawData);
    }
}
