package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

public class Mle1934Model extends VehicleModel<Mle1934Entity> {

    @Override
    public @Nullable TransformContext<Mle1934Entity> collectTransform(String boneName) {
        if (boneName.equals("barrel")) {
            return (bone, vehicle, state) -> {
                var entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    bone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                }
            };
        }

        return super.collectTransform(boneName);
    }

    @Override
    public boolean hideFor1stPassengerWhileZooming() {
        return true;
    }
}
