package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.MortarModel;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MortarRenderer extends GeoEntityRenderer<MortarEntity> {

    public MortarRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MortarModel());
        this.shadowRadius = 0f;
    }

    @Override
    public RenderType getRenderType(MortarEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(MortarEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public boolean shouldShowName(MortarEntity animatable) {
        return animatable.hasCustomName();
    }
}
