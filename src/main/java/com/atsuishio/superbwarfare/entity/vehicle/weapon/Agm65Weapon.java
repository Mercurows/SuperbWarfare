package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity;
import net.minecraft.world.entity.LivingEntity;

public class Agm65Weapon extends VehicleWeapon {

    public float damage = 1100, explosionDamage = 150, explosionRadius = 9;

    public Agm65Weapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/agm_65.png");
    }

    public Agm65Weapon damage(float damage) {
        this.damage = damage;
        return this;
    }
    public Agm65Weapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public Agm65Weapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public Agm65Entity create(LivingEntity entity) {
        return new Agm65Entity(entity, entity.level(), damage, explosionDamage, explosionRadius);
    }
}
