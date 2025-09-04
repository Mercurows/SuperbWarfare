package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class Desperado extends Perk {

    public Desperado() {
        super("desperado", Perk.Type.DAMAGE);
        appendModification(GunProp.RPM, (data, rpm) -> {
            if (data.perk.getTag(this).getInt("DesperadoTimePost") > 0) {
                return (int) (rpm * (1.285 + 0.015 * data.perk.getLevel(this)));
            }
            return rpm;
        });
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
        data.perk.reduceCooldown(this, "DesperadoTime");
        data.perk.reduceCooldown(this, "DesperadoTimePost");
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            data.perk.getTag(this).putInt("DesperadoTime", 90 + instance.level() * 10);
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        int time = data.perk.getTag(this).getInt("DesperadoTime");
        if (time > 0) {
            data.perk.getTag(this).remove("DesperadoTime");
            data.perk.getTag(this).putBoolean("Desperado", true);
        } else {
            data.perk.getTag(this).remove("Desperado");
        }
    }

    @Override
    public void postReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        if (!data.perk.getTag(this).getBoolean("Desperado")) {
            return;
        }
        data.perk.getTag(this).putInt("DesperadoTimePost", 110 + instance.level() * 10);
    }
}
