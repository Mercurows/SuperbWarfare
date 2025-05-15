package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;

public class TurboCharger extends Perk {

    public TurboCharger() {
        super("turbo_charger", Perk.Type.FUNCTIONAL);
    }

    @Override
    public int getModifiedCustomRPM(int rpm, GunData data, PerkInstance instance) {
        return Math.min(rpm + 5 + 3 * instance.level(), 1200);
    }
}
