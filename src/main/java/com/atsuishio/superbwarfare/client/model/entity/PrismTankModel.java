package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PrismTankModel extends GeoModel<PrismTankEntity> {

    @Override
    public ResourceLocation getAnimationResource(PrismTankEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(PrismTankEntity entity) {
        return Mod.loc("geo/prism_tank.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PrismTankEntity entity) {
        return Mod.loc("textures/entity/prism_tank.png");
    }
}
