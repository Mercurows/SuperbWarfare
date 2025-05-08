package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Mk82Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Mk82Model extends GeoModel<Mk82Entity> {

    @Override
    public ResourceLocation getAnimationResource(Mk82Entity entity) {
        return Mod.loc("animations/mk82.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Mk82Entity entity) {
        return Mod.loc("geo/mk82.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Mk82Entity entity) {
        return Mod.loc("textures/entity/mk82.png");
    }
}
