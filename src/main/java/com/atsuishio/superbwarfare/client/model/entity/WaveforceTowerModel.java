package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.CHARGED_ENERGY;
import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.WAVEFORCE_LENGTH;

public class WaveforceTowerModel extends VehicleModel<WaveforceTowerEntity> {

    int energy0 = 0;
    private final Pattern LIGHT_PATTERN = Pattern.compile("^light_(?<type>on|off)(?<id>\\d+)");

    @Override
    public @Nullable TransformContext<WaveforceTowerEntity> collectTransform(String boneName) {
        switch (boneName) {
            case "root" -> {
                return (bone, vehicle, state) -> {
                    var minecraft = Minecraft.getInstance();
                    var pCamera = minecraft.levelRenderer.getFrustum();

                    var aabb = vehicle.getBoundingBoxForCulling().inflate(0.5);
                    if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                        aabb = new AABB(vehicle.getX() - 6.0, vehicle.getY() - 4.0, vehicle.getZ() - 6.0, vehicle.getX() + 6.0, vehicle.getY() + 4.0, vehicle.getZ() + 6.0);
                    }

                    bone.setHidden(!pCamera.isVisible(aabb) && !RenderHelper.isInGui());
                };
            }

            case "laser" -> {
                return (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(WAVEFORCE_LENGTH));
            }
            case "glow2" -> {
                return (bone, vehicle, state) -> bone.setPosZ(-16 * vehicle.getEntityData().get(WAVEFORCE_LENGTH));
            }
            case "charge" -> {
                return (bone, vehicle, state) -> {
                    int energy = vehicle.getEntityData().get(CHARGED_ENERGY);
                    float energyRate = (float) energy / vehicle.maxChargeEnergy;
                    float energyRate0 = (float) energy0 / vehicle.maxChargeEnergy;

                    bone.setScaleZ(Mth.lerp(state.getPartialTick(), energyRate0, energyRate));
                    energy0 = energy;
                };
            }
        }

        var matcher = LIGHT_PATTERN.matcher(boneName);
        if (matcher.matches()) {
            var isOn = matcher.group("type").equals("on");
            var index = Integer.parseInt(matcher.group("id"));

            return (bone, vehicle, state) -> {
                int energy = vehicle.getEntityData().get(CHARGED_ENERGY);
                float energyRate = (float) energy / vehicle.maxChargeEnergy;
                var shouldTurnOn = energyRate >= index / 7f;

                bone.setHidden(shouldTurnOn != isOn);
            };
        }

        return super.collectTransform(boneName);
    }
}
