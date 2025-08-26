package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class MagnificentHowl extends Perk {

    public MagnificentHowl() {
        super("magnificent_howl", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, damage) -> data.perk.getTag(this).getInt("MagnificentHowlDamageCount") > 0 ? damage * 1.5 : damage);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isHeadshotDamage(source)) {
            data.perk.getTag(this).putInt("MagnificentHowlCount",
                    Math.min(data.perk.getTag(this).getInt("MagnificentHowlCount") + 1 + instance.level() / 5, 9 + instance.level()));
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable Entity living) {
        data.perk.getTag(this).putInt("MagnificentHowlDamageCount", data.perk.getTag(this).getInt("MagnificentHowlCount"));
        data.perk.getTag(this).remove("MagnificentHowlCount");
    }

    @Override
    public void onHurtEntity(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (data.perk.getTag(this).getInt("MagnificentHowlDamageCount") > 0) {
            data.perk.reduceCooldown(this, "MagnificentHowlDamageCount");
        }
    }
}
