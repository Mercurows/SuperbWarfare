package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class FourthTimesCharm extends Perk {

    public FourthTimesCharm() {
        super("fourth_times_charm", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.reduceCooldown(this, "FourthTimesCharmTick");

        var tag = data.perk.getTag(this);
        int count = tag.getInt("FourthTimesCharmCount");

        if (count >= 4) {
            tag.remove("FourthTimesCharmTick");
            tag.remove("FourthTimesCharmCount");

            int mag = data.magazine();
            data.ammo.set(Math.min(mag, data.ammo.get() + 2));
        }
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (source.getDirectEntity() instanceof ProjectileEntity projectile) {
            float bypassArmorRate = projectile.getBypassArmorRate();
            if (bypassArmorRate >= 1.0f && source.is(ModDamageTypes.GUN_FIRE_HEADSHOT_ABSOLUTE)) {
                handleFourthTimesCharm(data, instance);
            } else if (source.is(ModDamageTypes.GUN_FIRE_HEADSHOT)) {
                handleFourthTimesCharm(data, instance);
            }
        }
    }

    public void handleFourthTimesCharm(GunData data, PerkInstance instance) {
        int fourthTimesCharmTick = data.perk.getTag(this).getInt("FourthTimesCharmTick");
        if (fourthTimesCharmTick <= 0) {
            data.perk.getTag(this).putInt("FourthTimesCharmTick", 40 + 10 * instance.level());
            data.perk.getTag(this).putInt("FourthTimesCharmCount", 1);
        } else {
            int count = data.perk.getTag(this).getInt("FourthTimesCharmCount");
            if (count < 4) {
                data.perk.getTag(this).putInt("FourthTimesCharmCount", Math.min(4, count + 1));
            }
        }
    }
}
