package com.atsuishio.superbwarfare.entity.vehicle.base;

public interface AirEntity extends ArmedVehicleEntity {

    float getRotX(float tickDelta);

    float getRotY(float tickDelta);

    float getRotZ(float tickDelta);

    float getPower();

    int getDecoy();
}
