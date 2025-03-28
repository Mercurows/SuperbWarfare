package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.WgMissileEntity;
import net.minecraft.world.entity.LivingEntity;

public class WgMissileWeapon extends VehicleWeapon {

    public float damage = 250, explosionDamage = 200, explosionRadius = 10;

    public WgMissileWeapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/missile_9m113.png");
    }

    public WgMissileWeapon damage(float damage) {
        this.damage = damage;
        return this;
    }

    public WgMissileWeapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public WgMissileWeapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public WgMissileEntity create(LivingEntity entity) {
        return new WgMissileEntity(entity, entity.level(), damage, explosionDamage, explosionRadius);
    }
}
