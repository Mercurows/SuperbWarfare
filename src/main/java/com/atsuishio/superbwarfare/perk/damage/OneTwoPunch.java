package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class OneTwoPunch extends Perk {

    public OneTwoPunch() {
        super("one_two_punch", Perk.Type.DAMAGE);
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        if (gunData.perk.getTag(this).getInt("OneTwoPunchTime") > 0) {
            rawData.meleeDamage *= 1.5 + 0.75 * (gunData.perk.getLevel(this) - 1);
        }
        return super.computeProperties(gunData, rawData);
    }

    @Override
    public void onHit(LivingEntity attacker, GunData data, PerkInstance instance, Entity target) {
        super.onHit(attacker, data, instance, target);

        data.perk.getTag(this).putInt("OneTwoPunchCount", data.perk.getTag(this).getInt("OneTwoPunchCount") + 1);
        data.perk.getTag(this).putInt("OneTwoPunchCountTime", 2);

        double needCount = Math.floor(data.get(GunProp.PROJECTILE_AMOUNT) * (1 - 0.05 * (instance.level() - 1)));

        if (data.perk.getTag(this).getInt("OneTwoPunchCount") >= needCount) {
            data.perk.getTag(this).putInt("OneTwoPunchTime", 60);

            data.perk.getTag(this).remove("OneTwoPunchCount");
            data.perk.getTag(this).remove("OneTwoPunchCountTime");
        }
    }

    @Override
    public void onChangeSlot(GunData data, PerkInstance instance, @Nullable Entity living) {
        super.onChangeSlot(data, instance, living);
        data.perk.getTag(this).remove("OneTwoPunchTime");
        data.perk.getTag(this).remove("OneTwoPunchCount");
        data.perk.getTag(this).remove("OneTwoPunchCountTime");
    }

    @Override
    public void onMeleeAttack(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        super.onMeleeAttack(data, instance, target, source);
        data.perk.getTag(this).remove("OneTwoPunchTime");
        data.perk.getTag(this).remove("OneTwoPunchCount");
        data.perk.getTag(this).remove("OneTwoPunchCountTime");
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
        super.tick(data, instance, entity);
        data.perk.reduceCooldown(this, "OneTwoPunchTime");
        data.perk.reduceCooldown(this, "OneTwoPunchCountTime");
    }
}
