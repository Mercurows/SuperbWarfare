package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DroneModel extends GeoModel<DroneEntity> {
    @Override
    public ResourceLocation getAnimationResource(DroneEntity entity) {
        return Mod.loc("animations/drone.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(DroneEntity entity) {
        return Mod.loc("geo/drone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DroneEntity entity) {
        return Mod.loc("textures/entity/drone.png");
    }
}
