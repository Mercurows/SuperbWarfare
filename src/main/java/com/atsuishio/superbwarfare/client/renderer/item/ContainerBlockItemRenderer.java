package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.ContainerItemModel;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ContainerBlockItemRenderer extends GeoItemRenderer<ContainerBlockItem> {

    public ContainerBlockItemRenderer() {
        super(new ContainerItemModel());
    }

    @Override
    public RenderType getRenderType(ContainerBlockItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
