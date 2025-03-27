package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.FuMO25Block;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.model.block.FuMO25Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
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
        return true;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FuMO25BlockEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        String name = bone.getName();
        if (name.equals("mian") && animatable.getBlockState().getValue(FuMO25Block.POWERED)) {
            bone.setRotY((System.currentTimeMillis() % 36000000) / 1200f);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
