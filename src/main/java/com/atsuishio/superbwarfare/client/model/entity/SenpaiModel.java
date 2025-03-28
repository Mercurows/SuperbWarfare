package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SenpaiModel extends GeoModel<SenpaiEntity> {

    @Override
    public ResourceLocation getAnimationResource(SenpaiEntity entity) {
        return Mod.loc("animations/senpai.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SenpaiEntity entity) {
        return Mod.loc("geo/senpai.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SenpaiEntity entity) {
        return Mod.loc("textures/entity/senpai.png");
    }
}
