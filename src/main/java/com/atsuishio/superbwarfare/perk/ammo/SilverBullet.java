package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

public class SilverBullet extends AmmoPerk {

    public SilverBullet() {
        super(new AmmoPerk.Builder("silver_bullet", Perk.Type.AMMO).bypassArmorRate(0.05).damageRate(0.8).speedRate(1.1).rgb(87, 166, 219));
        appendModification(GunProp.DAMAGE, (data, value, target, source) -> target instanceof LivingEntity living && living.getMobType() == MobType.UNDEAD ?
                value * (1.0 + 0.5 * data.perk.getLevel(this)) : value);
    }
}
