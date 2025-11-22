package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import org.jetbrains.annotations.Nullable;

public class WheelChairModel extends VehicleModel<WheelChairEntity> {

    @Override
    public @Nullable TransformContext<WheelChairEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "w_rb" -> (bone, vehicle, state) -> bone.setRotX(vehicle.getRightWheelRot());
            case "w_lb" -> (bone, vehicle, state) -> bone.setRotX(vehicle.getLeftWheelRot());
            case "w_rr" -> (bone, vehicle, state) -> bone.setRotX(4 * vehicle.getRightWheelRot());
            case "w_lr" -> (bone, vehicle, state) -> bone.setRotX(4 * vehicle.getLeftWheelRot());
            default -> super.collectTransform(boneName);
        };
    }
}
