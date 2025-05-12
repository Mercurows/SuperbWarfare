package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;

public class SmallCannonShellWeapon extends VehicleWeapon {

    public float damage = 40, explosionDamage = 80, explosionRadius = 5;
    public boolean aa = false;
    public Explosion.BlockInteraction blockInteraction = null;

    public SmallCannonShellWeapon damage(float damage) {
        this.damage = damage;
        return this;
    }

    public SmallCannonShellWeapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public SmallCannonShellWeapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public SmallCannonShellWeapon aa(boolean aa) {
        this.aa = aa;
        return this;
    }

    public SmallCannonShellWeapon blockInteraction(Explosion.BlockInteraction blockInteraction) {
        this.blockInteraction = blockInteraction;
        return this;
    }

    public SmallCannonShellEntity create(LivingEntity entity) {
        return new SmallCannonShellEntity(entity, entity.level(), damage, explosionDamage, explosionRadius, aa).setBlockInteraction(blockInteraction);
    }
}
