package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

import static com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity.COOL_DOWN;
import static com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity.RIGHT_BARREL_ANIM;

public class Mle1934Model extends VehicleModel<Mle1934Entity> {

    @Override
    public @Nullable TransformContext<Mle1934Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "barrel" -> (bone, vehicle, state) -> {
                var entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    bone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                }
            };
            case "flare2" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(COOL_DOWN) <= 64);

            case "flare" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(RIGHT_BARREL_ANIM) <= 10);

            default -> super.collectTransform(boneName);
        };
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
