package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity;
import com.atsuishio.superbwarfare.client.model.block.SmallContainerBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SmallContainerBlockEntityRenderer extends GeoBlockRenderer<SmallContainerBlockEntity> {

    public SmallContainerBlockEntityRenderer() {
        super(new SmallContainerBlockModel());
    }

    @Override
    public RenderType getRenderType(SmallContainerBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
