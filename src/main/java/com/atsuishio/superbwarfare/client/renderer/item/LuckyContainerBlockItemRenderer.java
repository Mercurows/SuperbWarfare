package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.LuckyContainerItemModel;
import com.atsuishio.superbwarfare.item.common.container.LuckyContainerBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class LuckyContainerBlockItemRenderer extends GeoItemRenderer<LuckyContainerBlockItem> {

    public LuckyContainerBlockItemRenderer() {
        super(new LuckyContainerItemModel());
    }

    @Override
    public RenderType getRenderType(LuckyContainerBlockItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
