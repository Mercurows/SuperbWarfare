package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class SpeedboatModel extends VehicleModel<SpeedboatEntity> {

    @Override
    public @Nullable TransformContext<SpeedboatEntity> collectTransform(String boneName) {
        if (boneName.equals("propeller")) {
            return (bone, vehicle, state) -> bone.setRotZ(Mth.lerp(state.getPartialTick(), vehicle.getPropellerRotO(), vehicle.getPropellerRot()));
        }

        if (boneName.equals("rudder")) {
            return (bone, vehicle, state) -> bone.setRotY(Mth.lerp(state.getPartialTick(), vehicle.getRudderRotO(), vehicle.getRudderRot()));
        }

        return super.collectTransform(boneName);
    }
}
