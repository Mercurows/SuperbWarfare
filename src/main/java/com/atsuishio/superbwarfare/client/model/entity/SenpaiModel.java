package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SenpaiModel extends GeoModel<SenpaiEntity> {

    @Override
    public ResourceLocation getAnimationResource(SenpaiEntity entity) {
        return ModUtils.loc("animations/senpai.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SenpaiEntity entity) {
        return ModUtils.loc("geo/senpai.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SenpaiEntity entity) {
        return ModUtils.loc("textures/entity/senpai.png");
    }
}
