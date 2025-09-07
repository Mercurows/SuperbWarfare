package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HealClip extends Perk {

    public HealClip() {
        super("heal_clip", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
        data.perk.reduceCooldown(this, "HealClipTime");
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_EXPLOSION)) {
            int healClipLevel = instance.level();
            if (healClipLevel != 0) {
                data.perk.getTag(this).putInt("HealClipTime", 80 + healClipLevel * 20);
            }
        }
    }

    @Override
    public void preReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        int time = data.perk.getTag(this).getInt("HealClipTime");
        if (time > 0) {
            data.perk.getTag(this).remove("HealClipTime");
            data.perk.getTag(this).putBoolean("HealClip", true);
        } else {
            data.perk.getTag(this).remove("HealClip");
        }
    }

    @Override
    public void postReload(GunData data, PerkInstance instance, @Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;

        if (!data.perk.getTag(this).contains("HealClip")) {
            return;
        }

        int healClipLevel = instance.level();
        if (healClipLevel == 0) {
            healClipLevel = 1;
        }

        float healAmount = 12 * (0.8f + 0.2f * healClipLevel);
        float absorption = healAmount - living.getMaxHealth() + living.getHealth();
        living.heal(healAmount);
        if (absorption > 0) {
            living.setAbsorptionAmount(absorption * 0.3f);
        }

        List<Player> players = entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(5))
                .stream().filter(p -> p.isAlliedTo(entity) || (entity instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() == p)).toList();
        int finalHealClipLevel = healClipLevel;
        players.forEach(p -> p.heal(6.0f * (0.8f + 0.2f * finalHealClipLevel)));
    }
}
