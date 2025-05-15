package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class MagnificentHowl extends Perk {

    public MagnificentHowl() {
        super("magnificent_howl", Perk.Type.DAMAGE);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            data.perk.getTag(this).putInt("MagnificentHowlCount",
                    Math.min(data.perk.getTag(this).getInt("MagnificentHowlCount") + 1 + instance.level() / 5, 9 + instance.level()));
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.getTag(this).putInt("MagnificentHowlDamageCount", data.perk.getTag(this).getInt("MagnificentHowlCount"));
        data.perk.getTag(this).remove("MagnificentHowlCount");
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (data.perk.getTag(this).getInt("MagnificentHowlDamageCount") > 0) {
            data.perk.reduceCooldown(this, "MagnificentHowlDamageCount");
        }
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        if (data.perk.getTag(this).getInt("MagnificentHowlDamageCount") > 0) {
            return damage * 1.5f;
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
