package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.projectile.Tm62Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Tm62ItemModel extends GeoModel<Tm62Item> {

    @Override
    public ResourceLocation getAnimationResource(Tm62Item animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Tm62Item animatable) {
        return Mod.loc("geo/tm_62.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Tm62Item animatable) {
        return Mod.loc("textures/entity/tm_62.png");
    }
}
