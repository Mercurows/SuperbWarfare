package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Mk82Entity;
import net.minecraft.world.entity.LivingEntity;

public class Mk82Weapon extends VehicleWeapon {
    public Mk82Weapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/mk_82.png");
    }

    public Mk82Entity create(LivingEntity entity) {
        return new Mk82Entity(entity, entity.level());
    }
}
