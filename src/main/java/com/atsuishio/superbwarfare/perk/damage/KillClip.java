package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class KillClip extends Perk {

    public KillClip() {
        super("kill_clip", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, damage) -> data.perk.getTag(this).getInt("KillClipTime") > 0 ?
                damage * (1.2 + 0.05 * data.perk.getLevel(this)) : damage);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
        data.perk.reduceCooldown(this, "KillClipReloadTime");
        data.perk.reduceCooldown(this, "KillClipTime");
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        int time = data.perk.getTag(this).getInt("KillClipReloadTime");
        if (time > 0) {
            data.perk.getTag(this).remove("KillClipReloadTime");
            data.perk.getTag(this).putBoolean("KillClip", true);
        } else {
            data.perk.getTag(this).remove("KillClip");
        }
    }

    @Override
    public void postReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        if (!data.perk.getTag(this).getBoolean("KillClip")) {
            return;
        }

        int level = instance.level();
        data.perk.getTag(this).putInt("KillClipTime", 90 + 10 * level);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            int killClipLevel = instance.level();
            if (killClipLevel != 0) {
                data.perk.getTag(this).putInt("KillClipReloadTime", 80);
            }
        }
    }
}
