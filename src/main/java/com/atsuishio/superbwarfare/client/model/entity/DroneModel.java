package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import org.jetbrains.annotations.Nullable;

public class DroneModel extends VehicleModel<DroneEntity> {
    @Override
    public @Nullable TransformContext<DroneEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "wingFL", "wingFR", "wingBL", "wingBR" ->
                    (bone, vehicle, state) -> bone.setRotY((System.currentTimeMillis() % 36000000) / 12f);
            default -> super.collectTransform(boneName);
        };
    }
}
