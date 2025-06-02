package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.Tm62;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Tm62ItemModel extends GeoModel<Tm62> {

    @Override
    public ResourceLocation getAnimationResource(Tm62 animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Tm62 animatable) {
        return Mod.loc("geo/tm_62.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Tm62 animatable) {
        return Mod.loc("textures/entity/tm_62.png");
    }
}
