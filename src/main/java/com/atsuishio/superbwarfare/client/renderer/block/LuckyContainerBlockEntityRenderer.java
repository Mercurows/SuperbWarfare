package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity;
import com.atsuishio.superbwarfare.client.model.block.LuckyContainerBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class LuckyContainerBlockEntityRenderer extends GeoBlockRenderer<LuckyContainerBlockEntity> {

    public LuckyContainerBlockEntityRenderer() {
        super(new LuckyContainerBlockModel());
    }

    @Override
    public RenderType getRenderType(LuckyContainerBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
