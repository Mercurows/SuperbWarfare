package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;

public class WaveforceTowerModel extends VehicleModel<WaveforceTowerEntity> {
    float energy0 = 0;
    private final Pattern LIGHT_PATTERN = Pattern.compile("^light_(?<type>on|off)(?<id>\\d+)");

    @Override
    public @Nullable TransformContext<WaveforceTowerEntity> collectTransform(String boneName) {
        switch (boneName) {

            case "glow" -> {
                return (bone, vehicle, state) -> {
                    float scale = Math.min(Mth.lerp(state.getPartialTick(), vehicle.getEntityData().get(LASER_SCALE_O), vehicle.getEntityData().get(LASER_SCALE)), 1.2f);
                    bone.setScaleX(scale);
                    bone.setScaleY(scale);
                    bone.setScaleZ(scale);
                };
            }

            case "glow2" -> {
                return (bone, vehicle, state) -> {
                    bone.setPosZ(-16f * vehicle.getEntityData().get(LASER_LENGTH));
                    float scale = Math.min(Mth.lerp(state.getPartialTick(), vehicle.getEntityData().get(LASER_SCALE_O), vehicle.getEntityData().get(LASER_SCALE)), 1.2f);
                    bone.setScaleX(scale);
                    bone.setScaleY(scale);
                    bone.setScaleZ(scale);
                };
            }
            case "charge" -> {
                return (bone, vehicle, state) -> {
                    float energy = vehicle.getEntityData().get(CHARGE_PROGRESS);
                    float energyRate0 = energy0;
                    bone.setScaleZ(Mth.lerp(state.getPartialTick(), energyRate0, energy));
                    energy0 = energy;
                };
            }
        }

        var matcher = LIGHT_PATTERN.matcher(boneName);
        if (matcher.matches()) {
            var isOn = matcher.group("type").equals("on");
            var index = Integer.parseInt(matcher.group("id"));

            return (bone, vehicle, state) -> {
                float energy = vehicle.getEntityData().get(CHARGE_PROGRESS);
                var shouldTurnOn =  energy >= index / 7f;

                bone.setHidden(shouldTurnOn != isOn);
            };
        }

        return super.collectTransform(boneName);
    }
}
