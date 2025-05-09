package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Agm65Model extends GeoModel<Agm65Entity> {

    @Override
    public ResourceLocation getAnimationResource(Agm65Entity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Agm65Entity entity) {
        return Mod.loc("geo/agm65.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Agm65Entity entity) {
        return Mod.loc("textures/entity/agm65.png");
    }
}
