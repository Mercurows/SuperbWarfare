package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;

public class BrainStorm extends Perk {
    public BrainStorm() {
        super("brain_storm", Perk.Type.DAMAGE);
    }

    @Override
    public DefaultGunData computeProperties(GunData data, DefaultGunData rawData) {
        rawData.headshot += 0.25 * data.perk.getLevel(this);
        return super.computeProperties(data, rawData);
    }
}
