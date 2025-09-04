package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.PropModifier;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;

public class HEBullet extends AmmoPerk {

    public HEBullet() {
        super(new AmmoPerk.Builder("he_bullet", Perk.Type.AMMO).bypassArmorRate(-0.3f).damageRate(0.5f).speedRate(0.85f).slug().rgb(240, 20, 10));
        appendModification(GunProp.EXPLOSION_DAMAGE, (pm, data, value) -> (0.9 * ((PropModifier<GunData, DefaultGunData, Double>) pm).get(GunProp.DAMAGE) * 2) * (1 + 0.1 * data.perk.getLevel(this)));
        appendModification(GunProp.EXPLOSION_RADIUS, (pm, data, value) -> (1.7 + 0.3 * data.perk.getLevel(this)));
    }
}
