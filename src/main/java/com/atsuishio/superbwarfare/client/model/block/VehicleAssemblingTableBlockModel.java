package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VehicleAssemblingTableBlockModel extends GeoModel<VehicleAssemblingTableBlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(VehicleAssemblingTableBlockEntity animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(VehicleAssemblingTableBlockEntity animatable) {
        return Mod.loc("geo/vehicle_assembling_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleAssemblingTableBlockEntity animatable) {
        return Mod.loc("textures/block/vehicle_assembling_table.png");
    }
}
