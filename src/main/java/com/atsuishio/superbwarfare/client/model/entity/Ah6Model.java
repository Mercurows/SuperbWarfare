package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class Ah6Model extends VehicleModel<Ah6Entity> {

    @Override
    public @Nullable TransformContext<Ah6Entity> collectTransform(String boneName) {
        if (boneName.equals("propeller")) {
            return (bone, vehicle, state) -> bone.setRotY(Mth.lerp(state.getPartialTick(), vehicle.propellerRotO, vehicle.getPropellerRot()));
        }

        if (boneName.equals("tailPropeller")) {
            return (bone, vehicle, state) -> bone.setRotX(-6 * Mth.lerp(state.getPartialTick(), vehicle.propellerRotO, vehicle.getPropellerRot()));
        }

        return super.collectTransform(boneName);
    }
}
