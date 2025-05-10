package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity;
import net.minecraft.world.entity.LivingEntity;

public class Agm65Weapon extends VehicleWeapon {
    public Agm65Weapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/agm_65.png");
    }

    public Agm65Entity create(LivingEntity entity) {
        return new Agm65Entity(entity, entity.level());
    }
}
