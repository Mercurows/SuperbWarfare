package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.Tm62Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Tm62Model extends GeoModel<Tm62Entity> {

    @Override
    public ResourceLocation getAnimationResource(Tm62Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Tm62Entity entity) {
        return Mod.loc("geo/tm_62.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Tm62Entity entity) {
        return Mod.loc("textures/entity/tm_62.png");
    }
}
