package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class TowModel extends VehicleModel<TowEntity> {
    @Override
    public @Nullable TransformContext<TowEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "guanmiao" -> (bone, vehicle, state) -> {
                var player = Minecraft.getInstance().player;
                bone.setHidden(vehicle.getFirstPassenger() == player && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle));
            };
            case "turret" ->
                    (bone, vehicle, state) -> bone.setRotY(-Mth.lerp(state.getPartialTick(), vehicle.yRotO, vehicle.getYRot()) * Mth.DEG_TO_RAD);
            case "barrel" ->
                    (bone, vehicle, state) -> bone.setRotX(-Mth.lerp(state.getPartialTick(), vehicle.xRotO, vehicle.getXRot()) * Mth.DEG_TO_RAD);
            case "missile" ->
                    (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(TowEntity.STATE) == 0);
            default -> super.collectTransform(boneName);
        };
    }
}
