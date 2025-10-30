package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Ptkm1rModel;
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Ptkm1rRenderer extends GeoEntityRenderer<Ptkm1rEntity> {

    public Ptkm1rRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Ptkm1rModel());
    }

    @Override
    public RenderType getRenderType(Ptkm1rEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
