package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.entity.LivingEntity;

public class VorpalWeapon extends Perk {

    public VorpalWeapon() {
        super("vorpal_weapon", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, ((data, value, target, source) -> {
            if (source == null) return value;
            if ((DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_BOOM)) && target instanceof LivingEntity living && living.getHealth() >= 100.0f) {
                return value + living.getHealth() * 0.00002 * Math.pow(data.perk.getLevel(this), 2);
            }
            return value;
        }));
    }
}
