package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

public class FieldDoctor extends Perk {

    public FieldDoctor() {
        super("field_doctor", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void onHurtEntity(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        super.onHurtEntity(damage, data, instance, target, source);
        if (!trigger(target, source)) {
            return;
        }
        if (target instanceof LivingEntity living) {
            living.heal(damage * Math.min(1.0f, 0.25f + 0.05f * instance.level()));
        }
    }

    public boolean trigger(Entity target, DamageSource source) {
        if (source.getDirectEntity() instanceof ProjectileEntity projectile && !projectile.isZoom()) {
            LivingEntity attacker = null;

            if (source.getEntity() instanceof LivingEntity living) {
                if (living instanceof ServerPlayer player) {
                    attacker = player;
                } else {
                    attacker = living;
                }
            }
            if (projectile.getOwner() instanceof LivingEntity living) {
                if (living instanceof ServerPlayer player) {
                    attacker = player;
                } else if (living instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() instanceof ServerPlayer) {
                    attacker = living;
                }
            }

            return attacker != null && target != null &&
                    (target.isAlliedTo(attacker) || (attacker instanceof OwnableEntity ownableEntity && ownableEntity == target));
        }
        return false;
    }
}
