package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ContainerItemModel extends GeoModel<ContainerBlockItem> {

    @Override
    public ResourceLocation getAnimationResource(ContainerBlockItem animatable) {
        return Mod.loc("animations/container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(ContainerBlockItem animatable) {
        return Mod.loc("geo/container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ContainerBlockItem animatable) {
        return Mod.loc("textures/block/container.png");
    }
}
