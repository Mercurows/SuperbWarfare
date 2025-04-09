package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FuMO25Model extends GeoModel<FuMO25BlockEntity> {
    @Override
    public ResourceLocation getAnimationResource(FuMO25BlockEntity animatable) {
        return Mod.loc("animations/fumo_25.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(FuMO25BlockEntity animatable) {
        return Mod.loc("geo/fumo_25.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FuMO25BlockEntity animatable) {
        return Mod.loc("textures/block/fumo_25.png");
    }
}