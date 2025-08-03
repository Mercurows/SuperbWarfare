package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;

public class RiotBullet extends AmmoPerk {

    public RiotBullet() {
        super(new Builder("riot_bullet", Type.AMMO).bypassArmorRate(-0.3f).damageRate(0.9f).speedRate(0.8f).slug().rgb(70, 35, 230)
                .mobEffect(() -> MobEffects.MOVEMENT_SLOWDOWN).mobEffect(() -> MobEffects.WEAKNESS));
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if ((target != null && target.getType().is(EntityTypeTags.RAIDERS)) || target instanceof Vex) {
            return damage * (1 + 0.5f * instance.level());
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }

    @Override
    public int getEffectAmplifier(PerkInstance instance) {
        return (int) (instance.level() * 0.25);
    }

    @Override
    public int getEffectDuration(PerkInstance instance) {
        return 20 + instance.level() * 10;
    }
}
