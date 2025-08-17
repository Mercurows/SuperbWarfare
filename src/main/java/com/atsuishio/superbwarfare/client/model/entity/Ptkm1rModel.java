package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Ptkm1rModel extends GeoModel<Ptkm1rEntity> {

    @Override
    public ResourceLocation getAnimationResource(Ptkm1rEntity entity) {
        return Mod.loc("animations/ptkm_1r.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Ptkm1rEntity entity) {
        return Mod.loc("geo/ptkm_1r.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Ptkm1rEntity entity) {
        return Mod.loc("textures/entity/ptkm_1r.png");
    }
}
