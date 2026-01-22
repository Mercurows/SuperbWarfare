package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Sc50Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Sc50Model extends GeoModel<Sc50Entity> {

    @Override
    public ResourceLocation getAnimationResource(Sc50Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Sc50Entity entity) {
        return Mod.loc("geo/sc_50.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Sc50Entity entity) {
        return Mod.loc("textures/entity/ju_87.png");
    }
}
