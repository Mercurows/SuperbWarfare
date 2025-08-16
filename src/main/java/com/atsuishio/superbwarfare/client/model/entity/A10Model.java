package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class A10Model extends GeoModel<A10Entity> {

    @Override
    public ResourceLocation getAnimationResource(A10Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(A10Entity entity) {
        return Mod.loc("geo/a10.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(A10Entity entity) {
        return Mod.loc("textures/entity/a10.png");
    }
}
