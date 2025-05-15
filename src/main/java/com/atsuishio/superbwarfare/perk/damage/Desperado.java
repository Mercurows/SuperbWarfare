package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class Desperado extends Perk {

    public Desperado() {
        super("desperado", Perk.Type.DAMAGE);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.reduceCooldown(this, "DesperadoTime");
        data.perk.reduceCooldown(this, "DesperadoTimePost");
    }

    @Override
    public int getModifiedRPM(int rpm, GunData data, PerkInstance instance) {
        if (data.perk.getTag(this).getInt("DesperadoTimePost") > 0) {
            return (int) (rpm * (1.285 + 0.015 * instance.level()));
        }
        return super.getModifiedRPM(rpm, data, instance);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            data.perk.getTag(this).putInt("DesperadoTime", 90 + instance.level() * 10);
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        int time = data.perk.getTag(this).getInt("DesperadoTime");
        if (time > 0) {
            data.perk.getTag(this).remove("DesperadoTime");
            data.perk.getTag(this).putBoolean("Desperado", true);
        } else {
            data.perk.getTag(this).remove("Desperado");
        }
    }

    @Override
    public void postReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        if (!data.perk.getTag(this).getBoolean("Desperado")) {
            return;
        }
        data.perk.getTag(this).putInt("DesperadoTimePost", 110 + instance.level() * 10);
    }
}
