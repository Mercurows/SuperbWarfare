package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VehicleAssemblingTableVehicleModel extends GeoModel<VehicleAssemblingTableVehicleEntity> {

    @Override
    public ResourceLocation getAnimationResource(VehicleAssemblingTableVehicleEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(VehicleAssemblingTableVehicleEntity entity) {
        return Mod.loc("geo/vehicle_assembling_table_vehicle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleAssemblingTableVehicleEntity entity) {
        return Mod.loc("textures/entity/vehicle_assembling_table.png");
    }
}
