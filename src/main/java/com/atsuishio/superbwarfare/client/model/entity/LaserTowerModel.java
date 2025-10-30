package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity.LASER_LENGTH;

public class LaserTowerModel extends VehicleModel<LaserTowerEntity> {

    @Override
    public @Nullable TransformContext<LaserTowerEntity> collectTransform(String boneName) {
        if (boneName.equals("laser")) {
            return (bone, vehicle, state) -> bone.setScaleZ(10 * vehicle.getEntityData().get(LASER_LENGTH));
        }

        return super.collectTransform(boneName);
    }
}
