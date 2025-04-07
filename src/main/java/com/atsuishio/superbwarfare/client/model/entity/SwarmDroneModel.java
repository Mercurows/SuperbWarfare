package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SwarmDroneModel extends GeoModel<SwarmDroneEntity> {

    @Override
    public ResourceLocation getAnimationResource(SwarmDroneEntity entity) {
        return Mod.loc("animations/swarm_drone.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SwarmDroneEntity entity) {
        return Mod.loc("geo/swarm_drone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmDroneEntity entity) {
        return Mod.loc("textures/entity/swarm_drone.png");
    }
}
