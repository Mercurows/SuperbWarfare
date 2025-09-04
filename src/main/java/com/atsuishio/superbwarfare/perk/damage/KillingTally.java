package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class KillingTally extends Perk {

    public KillingTally() {
        super("killing_tally", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, damage) -> damage * (1 + (0.1 * data.perk.getLevel(this)) * data.perk.getTag(this).getInt("KillingTally")));
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        data.perk.getTag(this).remove("KillingTally");
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source)) {
            data.perk.getTag(this).putInt("KillingTally", Math.min(3, data.perk.getTag(this).getInt("KillingTally") + 1));
        }
    }

    @Override
    public void onChangeSlot(GunData data, PerkInstance instance, @Nullable Entity living) {
        data.perk.getTag(this).remove("KillingTally");
    }
}
