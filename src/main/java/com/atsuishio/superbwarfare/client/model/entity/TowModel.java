package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public class TowModel extends VehicleModel<TowEntity> {
    @Override
    public @Nullable TransformContext<TowEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "guanmiao" -> (bone, vehicle, state) -> {
                var player = Minecraft.getInstance().player;
                bone.setHidden(vehicle.getFirstPassenger() == player && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle));
            };
            case "missile" ->
                    (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(TowEntity.STATE) == 0);
            default -> super.collectTransform(boneName);
        };
    }
}
