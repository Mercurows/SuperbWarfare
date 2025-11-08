package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity.LASER_LENGTH;

public class LaserTowerModel extends VehicleModel<LaserTowerEntity> {

    @Override
    public @Nullable TransformContext<LaserTowerEntity> collectTransform(String boneName) {
        return switch (boneName) {
            case "laser" -> (bone, vehicle, state) -> bone.setScaleZ(10 * vehicle.getEntityData().get(LASER_LENGTH));
            case "root" -> (bone, vehicle, state) -> {
                var minecraft = Minecraft.getInstance();
                var pCamera = minecraft.levelRenderer.getFrustum();

                var aabb = vehicle.getBoundingBoxForCulling().inflate(0.5);
                if (aabb.hasNaN() || aabb.getSize() == 0) {
                    aabb = new AABB(vehicle.getX() - 2.0, vehicle.getY() - 2.0, vehicle.getZ() - 2.0, vehicle.getX() + 2.0, vehicle.getY() + 2.0, vehicle.getZ() + 2.0);
                }

                bone.setHidden(!pCamera.isVisible(aabb) && !RenderHelper.isInGui());

                super.collectTransform(boneName);
            };
            case "turret", "turret2" ->
                    (bone, vehicle, state) -> bone.setRotY(-Mth.lerp(state.getPartialTick(), vehicle.yRotO, vehicle.getYRot()) * Mth.DEG_TO_RAD);
            case "barrel", "barrel2" ->
                    (bone, vehicle, state) -> bone.setRotX(-Mth.lerp(state.getPartialTick(), vehicle.xRotO, vehicle.getXRot()) * Mth.DEG_TO_RAD);

            default -> super.collectTransform(boneName);
        };
    }
}
