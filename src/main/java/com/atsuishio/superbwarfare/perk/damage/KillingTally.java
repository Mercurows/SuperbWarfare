package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class KillingTally extends Perk {

    public KillingTally() {
        super("killing_tally", Perk.Type.DAMAGE);
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.getTag(this).remove("KillingTally");
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source)) {
            data.perk.getTag(this).putInt("KillingTally", Math.min(3, data.perk.getTag(this).getInt("KillingTally") + 1));
        }
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source)) {
            return damage * (1.0f + (0.1f * instance.level()) * data.perk.getTag(this).getInt("KillingTally"));
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }

    @Override
    public void onChangeSlot(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.getTag(this).remove("KillingTally");
    }
}
