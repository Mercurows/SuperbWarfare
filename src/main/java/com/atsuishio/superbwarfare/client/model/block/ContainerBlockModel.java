package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ContainerBlockModel extends GeoModel<ContainerBlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(ContainerBlockEntity animatable) {
        return ModUtils.loc("animations/container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(ContainerBlockEntity animatable) {
        return ModUtils.loc("geo/container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ContainerBlockEntity animatable) {
        return ModUtils.loc("textures/block/container.png");
    }
}
