package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public class Firefly extends Perk {

    public Firefly() {
        super("firefly", Perk.Type.DAMAGE);
    }

    @Override
    public void onKill(GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (!DamageTypeTool.isHeadshotDamage(source)) return;

        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
            attacker = player;
        }
        if (attacker == null) return;

        new CustomExplosion.Builder(target)
                .damage(6 + instance.level() * 2)
                .radius(2 + instance.level() * 0.5f)
                .directSource(attacker)
                .source(null)
                .keepBlock()
                .fireTime(3 + instance.level() / 3)
                .withParticleType(ParticleTool.ParticleType.SMALL)
                .explode();
    }
}
