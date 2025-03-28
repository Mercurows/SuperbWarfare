package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Tom6Model extends GeoModel<Tom6Entity> {

    @Override
    public ResourceLocation getAnimationResource(Tom6Entity entity) {
        return null;
//        return ModUtils.loc("animations/wheel_chair.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Tom6Entity entity) {
        return Mod.loc("geo/tom_6.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Tom6Entity entity) {
        return Mod.loc("textures/entity/tom_6.png");
    }
}
