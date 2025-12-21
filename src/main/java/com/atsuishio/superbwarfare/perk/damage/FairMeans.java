package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
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
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        if (gunData.perk.getTag(this).getBoolean("FairMeans")) {
            rawData.damage *= 1.5 + 0.225 * gunData.perk.getLevel(this);
        } else {
            rawData.damage *= 0.2 + 0.04 * gunData.perk.getLevel(this);
        }
        return super.computeProperties(gunData, rawData);
    }

    @Override
    public void onHurtEntity(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (data.get(GunProp.BYPASSES_ARMOR) > 0) {
            if (source.is(ModTags.DamageTypes.PROJECTILE_ABSOLUTE)) {
                data.perk.getTag(this).putBoolean("FairMeans", !data.perk.getTag(this).getBoolean("FairMeans"));
            }
        } else if (source.is(ModTags.DamageTypes.PROJECTILE)) {
            data.perk.getTag(this).putBoolean("FairMeans", !data.perk.getTag(this).getBoolean("FairMeans"));
        }
    }
}
