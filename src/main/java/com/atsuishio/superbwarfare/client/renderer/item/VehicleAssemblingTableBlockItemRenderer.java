package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.VehicleAssemblingTableItemModel;
import com.atsuishio.superbwarfare.item.VehicleAssemblingTableBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class VehicleAssemblingTableBlockItemRenderer extends GeoItemRenderer<VehicleAssemblingTableBlockItem> {

    public VehicleAssemblingTableBlockItemRenderer() {
        super(new VehicleAssemblingTableItemModel());
    }

    @Override
    public RenderType getRenderType(VehicleAssemblingTableBlockItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
