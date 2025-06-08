package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.phys.Vec3;

public interface AirEntity extends ArmedVehicleEntity {

    float getRotX(float tickDelta);

    float getRotY(float tickDelta);

    float getRotZ(float tickDelta);

    float getPower();

    int getDecoy();

    Vec3 shootPos(float tickDelta);

    Vec3 shootVec(float tickDelta);

}
