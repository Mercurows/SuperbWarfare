package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

import java.util.UUID;

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
        if (boneName.equals("gun")) {
            return (bone, vehicle, state) -> {
                var entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    bone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                }
            };
        }

        return super.collectTransform(boneName);
    }

    @Override
    public boolean hideFor1stPassengerWhileZooming() {
        return true;
    }
}
