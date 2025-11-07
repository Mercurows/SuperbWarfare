package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

import java.util.UUID;

import static com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity.*;

public class Bl132Model extends VehicleModel<Bl132Entity> {

    @Override
    public ResourceLocation getTextureResource(Bl132Entity entity) {
        UUID uuid = entity.getUUID();
        if (uuid.getLeastSignificantBits() % 50 == 0) {
            return Mod.loc("textures/entity/bl_132_black.png");
        }
        return Mod.loc("textures/entity/bl_132.png");
    }

    @Override
    public @Nullable TransformContext<Bl132Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "barrel" -> (bone, vehicle, state) -> {
                var entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    bone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                }
            };
            case "flare" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(COOL_DOWN) <= 75);
            case "flare2" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(BARREL_ANIM_2) <= 10);
            case "flare3" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(BARREL_ANIM_3) <= 10);
            case "flare4" -> (bone, vehicle, state) -> bone.setHidden(vehicle.getEntityData().get(BARREL_ANIM_4) <= 10);

            default -> super.collectTransform(boneName);
        };
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
