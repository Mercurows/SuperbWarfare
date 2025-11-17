package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Kh39Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Kh39Model extends GeoModel<Kh39Entity> {

    @Override
    public ResourceLocation getAnimationResource(Kh39Entity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Kh39Entity entity) {
        return Mod.loc("geo/kh_39.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Kh39Entity entity) {
        return Mod.loc("textures/entity/kh_39.png");
    }
}
