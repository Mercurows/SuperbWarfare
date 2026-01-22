package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Sc250Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Sc250Model extends GeoModel<Sc250Entity> {

    @Override
    public ResourceLocation getAnimationResource(Sc250Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Sc250Entity entity) {
        return Mod.loc("geo/sc_250.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Sc250Entity entity) {
        return Mod.loc("textures/entity/ju_87.png");
    }
}
