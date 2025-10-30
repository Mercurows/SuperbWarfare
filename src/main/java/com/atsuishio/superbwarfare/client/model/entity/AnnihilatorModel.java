package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
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
