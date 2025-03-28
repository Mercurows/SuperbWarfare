package com.atsuishio.superbwarfare.entity.vehicle.weapon;

public class SmallCannonShellWeapon extends VehicleWeapon {

    public float damage = 40, explosionDamage = 80, explosionRadius = 5;

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

    // TODO create
//    public SmallCannonShellEntity create(LivingEntity entity) {
//        return new SmallCannonShellEntity(entity, entity.level(), damage, explosionDamage, explosionRadius);
//    }
}
