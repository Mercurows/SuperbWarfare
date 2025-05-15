package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.EventHooks;

public class Firefly extends Perk {

    public Firefly() {
        super("firefly", Perk.Type.DAMAGE);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (!DamageTypeTool.isHeadshotDamage(source)) return;

        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
            attacker = player;
        }
        if (attacker == null) return;

        CustomExplosion explosion = new CustomExplosion(target.level(), attacker,
                ModDamageTypes.causeProjectileBoomDamage(target.level().registryAccess(), null, attacker),
                6 + instance.level() * 2, target.getX(), target.getY(), target.getZ(),
                2 + instance.level() * 0.5f, CustomExplosion.BlockInteraction.KEEP, false)
                .setFireTime(3 + instance.level() / 3);
        explosion.explode();
        EventHooks.onExplosionStart(target.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnSmallExplosionParticles(target.level(), target.position());
    }
}
