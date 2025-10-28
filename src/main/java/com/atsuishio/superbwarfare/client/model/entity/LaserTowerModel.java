package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity.LASER_LENGTH;

public class LaserTowerModel extends VehicleModel<LaserTowerEntity> {

    @Override
    public void setCustomAnimations(LaserTowerEntity animatable, long instanceId, AnimationState<LaserTowerEntity> animationState) {
        var laser = getAnimationProcessor().getBone("laser");
        laser.setScaleZ(10 * animatable.getEntityData().get(LASER_LENGTH));
    }
}
