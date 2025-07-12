package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Type63Model extends GeoModel<Type63Entity> {

    @Override
    public ResourceLocation getAnimationResource(Type63Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Type63Entity entity) {
        return Mod.loc("geo/type_63.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Type63Entity entity) {
        return Mod.loc("textures/entity/type_63.png");
    }
}
