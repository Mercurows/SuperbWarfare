package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity;
import com.atsuishio.superbwarfare.client.model.block.ContainerBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ContainerBlockEntityRenderer extends GeoBlockRenderer<ContainerBlockEntity> {

    public ContainerBlockEntityRenderer() {
        super(new ContainerBlockModel());
    }

    @Override
    public RenderType getRenderType(ContainerBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
