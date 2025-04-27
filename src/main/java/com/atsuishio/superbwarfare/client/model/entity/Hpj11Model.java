package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Hpj11Model extends GeoModel<Hpj11Entity> {

    @Override
    public ResourceLocation getAnimationResource(Hpj11Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Hpj11Entity entity) {
        return Mod.loc("geo/1130.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Hpj11Entity entity) {
        return Mod.loc("textures/entity/1130.png");
    }
}
