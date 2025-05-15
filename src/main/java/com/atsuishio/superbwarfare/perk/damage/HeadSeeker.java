package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class HeadSeeker extends Perk {

    public HeadSeeker() {
        super("head_seeker", Perk.Type.DAMAGE);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.reduceCooldown(this, "HeadSeeker");
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isGunFireDamage(source)) {
            data.perk.getTag(this).putInt("HeadSeeker", 11 + instance.level() * 2);
        }
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            if (data.perk.getTag(this).getInt("HeadSeeker") > 0) {
                return damage * (1.095f + 0.0225f * instance.level());
            }
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
