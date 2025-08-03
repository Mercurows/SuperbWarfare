package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SilverBullet extends AmmoPerk {

    public SilverBullet() {
        super(new AmmoPerk.Builder("silver_bullet", Perk.Type.AMMO).bypassArmorRate(0.05).damageRate(0.8).speedRate(1.1).rgb(87, 166, 219));
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (target instanceof LivingEntity living && living.getType().is(EntityTypeTags.UNDEAD)) {
            return damage * (1 + 0.5f * instance.level());
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
