package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;

public class PowerfulCooler extends Perk {

    public PowerfulCooler() {
        super("powerful_cooler", Perk.Type.FUNCTIONAL);
        appendModification(GunProp.NATURAL_COOLDOWN, (data, value) -> value * (1 + 0.05 * data.perk.getLevel(this)));
        appendModification(GunProp.HEAT_PER_SHOOT, (data, value) -> value * (1 - 0.02 * data.perk.getLevel(this)));
    }
}
