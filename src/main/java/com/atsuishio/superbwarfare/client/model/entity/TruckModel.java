package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import net.minecraft.resources.ResourceLocation;

import static com.atsuishio.superbwarfare.entity.vehicle.TruckEntity.GREEN;

public class TruckModel extends VehicleModel<TruckEntity> {

    @Override
    public ResourceLocation getTextureResource(TruckEntity entity) {
        if (entity.getEntityData().get(GREEN)) {
            return Mod.loc("textures/entity/truck_green.png");
        }
        return Mod.loc("textures/entity/truck_red.png");
    }
}
