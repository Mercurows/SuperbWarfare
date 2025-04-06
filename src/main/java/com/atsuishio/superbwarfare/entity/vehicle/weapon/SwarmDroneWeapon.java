package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import net.minecraft.world.entity.LivingEntity;

public class SwarmDroneWeapon extends VehicleWeapon {

    public float explosionDamage = 125, explosionRadius = 6;

    public SwarmDroneWeapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/swarm_drone.png");
    }

    public SwarmDroneWeapon explosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
        return this;
    }

    public SwarmDroneWeapon explosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
        return this;
    }

    public SwarmDroneEntity create(LivingEntity entity) {
        return new SwarmDroneEntity(entity, entity.level(), explosionDamage, explosionRadius);
    }
}
