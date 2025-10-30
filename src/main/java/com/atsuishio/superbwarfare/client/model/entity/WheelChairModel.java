package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import org.jetbrains.annotations.Nullable;

public class WheelChairModel extends VehicleModel<WheelChairEntity> {

    @Override
    public @Nullable TransformContext<WheelChairEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "w_rb" -> (bone, vehicle, state) -> bone.setRotX(vehicle.rightWheelRot);
            case "w_lb" -> (bone, vehicle, state) -> bone.setRotX(vehicle.leftWheelRot);
            case "w_rr" -> (bone, vehicle, state) -> bone.setRotX(4 * vehicle.rightWheelRot);
            case "w_lr" -> (bone, vehicle, state) -> bone.setRotX(4 * vehicle.leftWheelRot);
            default -> super.collectTransform(boneName);
        };
    }
}
