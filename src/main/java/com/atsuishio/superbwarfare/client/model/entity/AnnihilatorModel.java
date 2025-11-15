package com.atsuishio.superbwarfare.client.model.entity;

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
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_LEFT_LENGTH));
            case "laser2" ->
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_MIDDLE_LENGTH));
            case "laser3" ->
                    (bone, vehicle, state) -> bone.setScaleZ(vehicle.getEntityData().get(LASER_RIGHT_LENGTH));

            default -> {
                var matcher = LED_PATTERN.matcher(boneName);
                if (matcher.matches()) {
                    var isGreen = matcher.group("type").equals("green");
                    var id = Integer.parseInt(matcher.group("id"));

                    yield (bone, vehicle, state) -> {
                        float charge = vehicle.getEntityData().get(CHARGE_PROGRESS);
                        boolean cantShoot = charge > 1;

                        var hideGreen = 5 * charge < id || cantShoot;
                        bone.setHidden(isGreen == hideGreen);
                    };
                }

                yield super.collectTransform(boneName);
            }
        };
    }

//    @Override
//    public boolean hideForTurretControllerWhileZooming() {
//        return true;
//    }
}
