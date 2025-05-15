package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HealClip extends Perk {

    public HealClip() {
        super("heal_clip", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        data.perk.reduceCooldown(this, "HealClipTime");
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            int healClipLevel = instance.level();
            if (healClipLevel != 0) {
                data.perk.getTag(this).putInt("HealClipTime", 80 + healClipLevel * 20);
            }
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        int time = data.perk.getTag(this).getInt("HealClipTime");
        if (time > 0) {
            data.perk.getTag(this).remove("HealClipTime");
            data.perk.getTag(this).putBoolean("HealClip", true);
        } else {
            data.perk.getTag(this).remove("HealClip");
        }
    }

    @Override
    public void postReload(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        if (living == null) return;

        if (!data.perk.getTag(this).contains("HealClip")) {
            return;
        }

        int healClipLevel = instance.level();
        if (healClipLevel == 0) {
            healClipLevel = 1;
        }

        living.heal(12.0f * (0.8f + 0.2f * healClipLevel));
        List<Player> players = living.level().getEntitiesOfClass(Player.class, living.getBoundingBox().inflate(5))
                .stream().filter(p -> p.isAlliedTo(living)).toList();
        int finalHealClipLevel = healClipLevel;
        players.forEach(p -> p.heal(6.0f * (0.8f + 0.2f * finalHealClipLevel)));
    }
}
