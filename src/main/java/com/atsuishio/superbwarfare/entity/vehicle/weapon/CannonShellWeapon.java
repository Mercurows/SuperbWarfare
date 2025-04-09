package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import net.minecraft.world.entity.player.Player;

public class CannonShellWeapon extends VehicleWeapon {
    public float hitDamage, explosionRadius, explosionDamage, fireProbability, velocity, gravity;
    public int fireTime, durability;

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

    public CannonShellEntity create(Player player) {
        return new CannonShellEntity(player,
                player.level(),
                this.hitDamage,
                this.explosionRadius,
                this.explosionDamage,
                this.fireProbability,
                this.fireTime,
                this.gravity
        ).durability(this.durability);
    }
}
