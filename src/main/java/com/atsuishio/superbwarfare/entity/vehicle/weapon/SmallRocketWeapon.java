package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity;
import net.minecraft.world.entity.LivingEntity;

public class SmallRocketWeapon extends VehicleWeapon {

    public float damage = 140, explosionDamage = 60, explosionRadius = 5;

    public SmallRocketWeapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/small_rocket.png");
    }

    public SmallRocketWeapon damage(float damage) {
        this.damage = damage;
        return this;
    }

    public SmallRocketWeapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public SmallRocketWeapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public SmallRocketEntity create(LivingEntity entity) {
        return new SmallRocketEntity(entity, entity.level(), damage, explosionDamage, explosionRadius);
    }
}
