package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class HeadSeeker extends Perk {

    public HeadSeeker() {
        super("head_seeker", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE,
                (data, value, target, source) -> source != null && DamageTypeTool.isHeadshotDamage(source) && data.perk.getTag(this).getInt("HeadSeeker") > 0 ?
                        value * (1.095 + 0.0225 * data.perk.getLevel(this)) : value);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity living) {
        data.perk.reduceCooldown(this, "HeadSeeker");
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isGunFireDamage(source)) {
            data.perk.getTag(this).putInt("HeadSeeker", 11 + instance.level() * 2);
        }
    }
}
