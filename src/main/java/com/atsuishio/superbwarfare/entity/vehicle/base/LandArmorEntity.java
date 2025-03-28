package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.phys.Vec3;

public interface LandArmorEntity extends ArmedVehicleEntity {

    float turretYRotO();

    float turretYRot();

    float turretXRotO();

    float turretXRot();

    Vec3 getBarrelVec(float ticks);

    Vec3 getGunVec(float ticks);
}
