package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VehicleAssemblingTableItemModel extends GeoModel<VehicleAssemblingTableBlockItem> {

    @Override
    public ResourceLocation getAnimationResource(VehicleAssemblingTableBlockItem animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(VehicleAssemblingTableBlockItem animatable) {
        return Mod.loc("geo/vehicle_assembling_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleAssemblingTableBlockItem animatable) {
        return Mod.loc("textures/block/vehicle_assembling_table.png");
    }
}
