package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.phys.Vec3;

public interface AircraftEntity extends AirEntity {

    Vec3 shootPos(float tickDelta);

    Vec3 shootVec(float tickDelta);

    Float gearRot(float tickDelta);
}
