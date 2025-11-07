package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

public class Mk42Model extends VehicleModel<Mk42Entity> {

    @Override
    public @Nullable TransformContext<Mk42Entity> collectTransform(String boneName) {
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
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
