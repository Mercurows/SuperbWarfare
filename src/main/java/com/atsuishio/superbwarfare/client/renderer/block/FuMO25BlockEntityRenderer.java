package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.model.block.FuMO25Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;


public class FuMO25BlockEntityRenderer extends GeoBlockRenderer<FuMO25BlockEntity> {
    public FuMO25BlockEntityRenderer() {
        super(new FuMO25Model());
    }

    @Override
    public RenderType getRenderType(FuMO25BlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull FuMO25BlockEntity pBlockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }
}
