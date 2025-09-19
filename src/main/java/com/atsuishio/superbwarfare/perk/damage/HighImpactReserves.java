package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;

public class HighImpactReserves extends Perk {

    public HighImpactReserves() {
        super("high_impact_reserves", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (pm, data, damage) -> {
            double rate = (double) data.ammo.get() / Math.max(1, pm.<Integer>get(GunProp.MAGAZINE));
            int level = data.perk.getLevel(this);
            double limit = 0.5 + (level - 1) * 0.02;
            if (rate <= limit) {
                double min1 = 0.12;
                double max1 = 0.25;

                double min20 = 0.75;
                double max20 = 1.5;

                double t = (level - 1) / 19.0;

                double minOutput = min1 + t * (min20 - min1);
                double maxOutput = max1 + t * (max20 - max1);

                return damage * (1 + (1 - (rate / limit)) * (maxOutput - minOutput) + minOutput);
            }
            return damage;
        });
    }
}
