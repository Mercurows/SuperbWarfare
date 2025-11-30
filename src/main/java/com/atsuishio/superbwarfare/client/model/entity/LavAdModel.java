package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity;
import org.jetbrains.annotations.Nullable;

public class LavAdModel extends VehicleModel<LavAdEntity> {

    @Override
    public @Nullable TransformContext<LavAdEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "rot_barrel" -> (bone, vehicle, state) -> {
                var gunData = vehicle.getGunData(0, 0);

                if (gunData != null) {
                    bone.setRotZ(bone.getRotZ() + 0.3f * gunData.shootTimer.get());
                }
            };

            case "flare" -> (bone, vehicle, state) -> {
                var gunData = vehicle.getGunData(0, 0);

                if (gunData != null) {
                    bone.setHidden(gunData.shootTimer.get() <= 2);
                } else {
                    bone.setHidden(true);
                }

                bone.setScaleX((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setScaleY((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            };

            default -> super.collectTransform(boneName);
        };

    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
