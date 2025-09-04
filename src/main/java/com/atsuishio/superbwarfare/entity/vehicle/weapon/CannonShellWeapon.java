package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import net.minecraft.world.entity.LivingEntity;

public class CannonShellWeapon extends VehicleWeapon {
    public float hitDamage, explosionRadius, explosionDamage, fireProbability, velocity, gravity;
    public int fireTime, durability, spreadAmount, spreadAngle, spreadTime;


    public CannonShellEntity.Type type;

    public CannonShellWeapon hitDamage(float hitDamage) {
        this.hitDamage = hitDamage;
        return this;
    }

    public CannonShellWeapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public CannonShellWeapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public CannonShellWeapon fireProbability(float fireProbability) {
        this.fireProbability = fireProbability;
        return this;
    }

    public CannonShellWeapon velocity(float velocity) {
        this.velocity = velocity;
        return this;
    }

    public CannonShellWeapon fireTime(int fireTime) {
        this.fireTime = fireTime;
        return this;
    }

    public CannonShellWeapon durability(int durability) {
        this.durability = durability;
        return this;
    }

    public CannonShellWeapon gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public CannonShellWeapon type(CannonShellEntity.Type type) {
        this.type = type;
        return this;
    }

    public CannonShellWeapon spreadAmount(int spreadAmount) {
        this.spreadAmount = spreadAmount;
        return this;
    }

    public CannonShellWeapon spreadAngle(int spreadAngle) {
        this.spreadAngle = spreadAngle;
        return this;
    }

    public CannonShellWeapon spreadTime(int spreadTime) {
        this.spreadTime = spreadTime;
        return this;
    }

    public CannonShellEntity create(LivingEntity living) {
        return new CannonShellEntity(living,
                living.level(),
                this.hitDamage,
                this.explosionRadius,
                this.explosionDamage,
                this.fireProbability,
                this.fireTime,
                this.gravity,
                this.type,
                this.spreadAmount,
                this.spreadTime,
                this.spreadAngle
        ).durability(this.durability);
    }
}
