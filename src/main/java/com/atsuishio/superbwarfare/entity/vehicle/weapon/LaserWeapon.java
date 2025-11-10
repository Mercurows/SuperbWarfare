package com.atsuishio.superbwarfare.entity.vehicle.weapon;

import com.atsuishio.superbwarfare.Mod;

@Deprecated(forRemoval = true, since = "0.8.9")
public class LaserWeapon extends VehicleWeapon {

    public LaserWeapon() {
        this.icon = Mod.loc("textures/screens/vehicle_weapon/laser.png");
    }

}
