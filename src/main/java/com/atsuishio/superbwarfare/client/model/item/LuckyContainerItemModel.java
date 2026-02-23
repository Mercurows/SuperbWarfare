package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LuckyContainerItemModel extends GeoModel<LuckyContainerBlockItem> {

    @Override
    public ResourceLocation getAnimationResource(LuckyContainerBlockItem animatable) {
        return Mod.loc("animations/container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(LuckyContainerBlockItem animatable) {
        return Mod.loc("geo/container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LuckyContainerBlockItem animatable) {
        return Mod.loc("textures/block/lucky_container.png");
    }
}
