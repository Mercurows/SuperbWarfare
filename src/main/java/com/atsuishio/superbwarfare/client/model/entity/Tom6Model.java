package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity.MELON;

public class Tom6Model extends VehicleModel<Tom6Entity> {
    @Override
    public @Nullable TransformContext<Tom6Entity> collectTransform(String boneName) {
        if (boneName.equals("melon")) {
            return (bone, vehicle, state) -> bone.setHidden(!vehicle.getEntityData().get(MELON));
        }

        return super.collectTransform(boneName);
    }
}
