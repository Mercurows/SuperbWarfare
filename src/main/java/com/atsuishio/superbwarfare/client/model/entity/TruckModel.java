package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.entity.vehicle.TruckEntity.GREEN;

public class TruckModel extends GeoModel<TruckEntity> {

    @Override
    public ResourceLocation getAnimationResource(TruckEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(TruckEntity entity) {
        return Mod.loc("geo/truck.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TruckEntity entity) {
        if (entity.getEntityData().get(GREEN)) {
            return Mod.loc("textures/entity/truck_green.png");
        }
        return Mod.loc("textures/entity/truck_red.png");
    }
}
