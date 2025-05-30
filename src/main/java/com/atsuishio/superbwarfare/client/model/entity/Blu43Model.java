package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.Blu43Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Blu43Model extends GeoModel<Blu43Entity> {

    @Override
    public ResourceLocation getAnimationResource(Blu43Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Blu43Entity entity) {
        return Mod.loc("geo/blu_43.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Blu43Entity entity) {
        return Mod.loc("textures/entity/blu_43.png");
    }
}
