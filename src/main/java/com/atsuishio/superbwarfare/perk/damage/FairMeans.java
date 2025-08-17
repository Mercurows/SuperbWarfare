package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class FairMeans extends Perk {

    public FairMeans() {
        super("fair_means", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, damage) -> {
            if (data.perk.getTag(this).getBoolean("FairMeans")) {
                return (1.5 + 0.225 * data.perk.getLevel(this)) * damage;
            }
            return (0.2 + 0.04 * data.perk.getLevel(this)) * damage;
        });
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (data.get(GunProp.BYPASSES_ARMOR) > 0) {
            if (source.is(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
                data.perk.getTag(this).putBoolean("FairMeans", !data.perk.getTag(this).getBoolean("FairMeans"));
            }
        } else if (source.is(ModTags.DamageTypes.PROJECTILE)) {
            data.perk.getTag(this).putBoolean("FairMeans", !data.perk.getTag(this).getBoolean("FairMeans"));
        }
    }
}
