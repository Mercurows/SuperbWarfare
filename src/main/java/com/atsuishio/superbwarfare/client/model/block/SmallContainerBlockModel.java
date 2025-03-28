package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SmallContainerBlockModel extends GeoModel<SmallContainerBlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(SmallContainerBlockEntity animatable) {
        return Mod.loc("animations/small_container.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SmallContainerBlockEntity animatable) {
        return Mod.loc("geo/small_container.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallContainerBlockEntity animatable) {
        if (animatable.lootTableSeed != 0L && animatable.lootTableSeed % 205 == 0) {
            return Mod.loc("textures/block/small_container_sui.png");
        }
        return Mod.loc("textures/block/small_container.png");
    }
}
