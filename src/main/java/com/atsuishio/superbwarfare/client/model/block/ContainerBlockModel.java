package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ContainerBlockModel extends GeoModel<ContainerBlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(ContainerBlockEntity animatable) {
        return Mod.loc("animations/container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(ContainerBlockEntity animatable) {
        return Mod.loc("geo/container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ContainerBlockEntity animatable) {
        return Mod.loc("textures/block/container.png");
    }
}
