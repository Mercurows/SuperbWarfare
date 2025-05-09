package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Mk82Entity;
import net.minecraft.world.entity.LivingEntity;

public class Mk82Weapon extends VehicleWeapon {

    public float explosionDamage = 520, explosionRadius = 14;

    public Mk82Weapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/mk_82.png");
    }
    public Mk82Weapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public Mk82Weapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public Mk82Entity create(LivingEntity entity) {
        return new Mk82Entity(entity, entity.level(), explosionDamage, explosionRadius);
    }
}
