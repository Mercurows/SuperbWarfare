package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import org.jetbrains.annotations.Nullable;

public class A10Model extends VehicleModel<A10Entity> {

    @Override
    public @Nullable TransformContext<A10Entity> collectTransform(String boneName) {
        if (boneName.equals("root")) {
            return (bone, vehicle, state) -> bone.setHidden(hideFor1stPassengerWhileZooming && vehicle.getWeaponIndex(0) == 2);
        }

        return null;
    }
}
