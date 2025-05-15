package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public class FieldDoctor extends Perk {

    public FieldDoctor() {
        super("field_doctor", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        if (!trigger(target, source)) {
            return;
        }
        target.heal(damage * Math.min(1.0f, 0.25f + 0.05f * instance.level()));
    }

    @Override
    public boolean shouldCancelHurtEvent(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        return trigger(target, source);
    }

    public boolean trigger(LivingEntity target, DamageSource source) {
        if (source.getDirectEntity() instanceof ProjectileEntity projectile && !projectile.isZoom()) {
            Player attacker = null;
            if (source.getEntity() instanceof Player player) {
                attacker = player;
            }
            if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
                attacker = player;
            }
            return attacker != null && target != null && target.isAlliedTo(attacker);
        }
        return false;
    }
}
