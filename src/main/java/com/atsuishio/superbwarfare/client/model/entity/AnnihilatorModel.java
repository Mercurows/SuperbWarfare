package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity.*;

public class AnnihilatorModel extends VehicleModel<AnnihilatorEntity> {

    private final Pattern LED_PATTERN = Pattern.compile("led(?<type>green|red)(?<id>\\d+)");

    @Override
    public @Nullable TransformContext<AnnihilatorEntity> collectTransform(String boneName) {

        return switch (boneName) {
            case "laser1" ->
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_LEFT_LENGTH) + 0.5f);
            case "laser2" ->
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_MIDDLE_LENGTH) + 0.5f);
            case "laser3" ->
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_RIGHT_LENGTH) + 0.5f);
            case "bone" -> (bone, vehicle, state) -> {
                var minecraft = Minecraft.getInstance();
                var pCamera = minecraft.levelRenderer.getFrustum();

                var aabb = vehicle.getBoundingBoxForCulling().inflate(0.5);
                if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                    aabb = new AABB(vehicle.getX() - 6.0, vehicle.getY() - 4.0, vehicle.getZ() - 6.0, vehicle.getX() + 6.0, vehicle.getY() + 4.0, vehicle.getZ() + 6.0);
                }

                bone.setHidden(!pCamera.isVisible(aabb) && !RenderHelper.isInGui());
            };
            case "main", "main2" ->
                    (bone, vehicle, state) -> bone.setRotY(-Mth.lerp(state.getPartialTick(), vehicle.yRotO, vehicle.getYRot()) * Mth.DEG_TO_RAD);
            case "PaoGuan", "PaoGuan2" ->
                    (bone, vehicle, state) -> bone.setRotX(-Mth.lerp(state.getPartialTick(), vehicle.xRotO, vehicle.getXRot()) * Mth.DEG_TO_RAD);
            default -> {
                var matcher = LED_PATTERN.matcher(boneName);
                if (matcher.matches()) {
                    var isGreen = matcher.group("type").equals("green");
                    var id = Integer.parseInt(matcher.group("id"));

                    yield (bone, vehicle, state) -> {
                        float coolDown = vehicle.getEntityData().get(COOL_DOWN);
                        boolean cantShoot = vehicle.getEnergy() < VehicleConfig.ANNIHILATOR_SHOOT_COST.get();

                        var hideGreen = coolDown > (100 - id * 20) || cantShoot;
                        bone.setHidden(isGreen == hideGreen);
                    };
                }

                yield super.collectTransform(boneName);
            }
        };
    }
}
