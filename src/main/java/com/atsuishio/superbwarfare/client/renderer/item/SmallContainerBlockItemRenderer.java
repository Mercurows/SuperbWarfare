package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.SmallContainerItemModel;
import com.atsuishio.superbwarfare.item.SmallContainerBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SmallContainerBlockItemRenderer extends GeoItemRenderer<SmallContainerBlockItem> {

    public SmallContainerBlockItemRenderer() {
        super(new SmallContainerItemModel());
    }

    @Override
    public RenderType getRenderType(SmallContainerBlockItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
