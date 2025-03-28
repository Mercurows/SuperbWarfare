package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.SmallContainerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SmallContainerItemModel extends GeoModel<SmallContainerBlockItem> {

    @Override
    public ResourceLocation getAnimationResource(SmallContainerBlockItem animatable) {
        return Mod.loc("animations/small_container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SmallContainerBlockItem animatable) {
        return Mod.loc("geo/small_container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallContainerBlockItem animatable) {
        return Mod.loc("textures/block/small_container.png");
    }
}
