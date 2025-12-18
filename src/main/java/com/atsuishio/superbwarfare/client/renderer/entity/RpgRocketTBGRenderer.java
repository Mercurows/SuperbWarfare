package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.RpgRocketTBGLayer;
import com.atsuishio.superbwarfare.client.model.entity.RpgRocketTBGModel;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RpgRocketTBGRenderer extends GeoEntityRenderer<RpgRocketTBGEntity> {
    public RpgRocketTBGRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RpgRocketTBGModel());
        this.addRenderLayer(new RpgRocketTBGLayer(this));
    }

    @Override
    public RenderType getRenderType(RpgRocketTBGEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(RpgRocketTBGEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(RpgRocketTBGEntity entityLivingBaseIn) {
        return 0;
    }
}
