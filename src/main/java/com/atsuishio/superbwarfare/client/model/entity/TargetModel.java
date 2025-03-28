package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TargetModel extends GeoModel<TargetEntity> {

    @Override
    public ResourceLocation getAnimationResource(TargetEntity entity) {
        return Mod.loc("animations/target.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(TargetEntity entity) {
        return Mod.loc("geo/target.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TargetEntity entity) {
        return Mod.loc("textures/entity/target.png");
    }
}
