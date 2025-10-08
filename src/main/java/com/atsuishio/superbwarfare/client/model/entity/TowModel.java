package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TowModel extends GeoModel<TowEntity> {

    @Override
    public ResourceLocation getAnimationResource(TowEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(TowEntity entity) {
        return Mod.loc("geo/tow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TowEntity entity) {
        return Mod.loc("textures/entity/tow.png");
    }
}
