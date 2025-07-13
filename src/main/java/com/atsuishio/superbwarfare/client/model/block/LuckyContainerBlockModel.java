package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LuckyContainerBlockModel extends GeoModel<LuckyContainerBlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(LuckyContainerBlockEntity animatable) {
        return Mod.loc("animations/container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(LuckyContainerBlockEntity animatable) {
        return Mod.loc("geo/container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LuckyContainerBlockEntity animatable) {
        return Mod.loc("textures/block/lucky_container.png");
    }
}
