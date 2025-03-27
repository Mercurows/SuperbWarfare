package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.SmallContainerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SmallContainerItemModel extends GeoModel<SmallContainerBlockItem> {

    @Override
    public ResourceLocation getAnimationResource(SmallContainerBlockItem animatable) {
        return ModUtils.loc("animations/small_container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SmallContainerBlockItem animatable) {
        return ModUtils.loc("geo/small_container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallContainerBlockItem animatable) {
        return ModUtils.loc("textures/block/small_container.png");
    }
}
