package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.Ru9m336MissileLayer;
import com.atsuishio.superbwarfare.client.model.entity.Ru9m336MissileModel;
import com.atsuishio.superbwarfare.entity.projectile.Ru9m336MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Ru9m336MissileRenderer extends GeoEntityRenderer<Ru9m336MissileEntity> {
    public Ru9m336MissileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Ru9m336MissileModel());
        this.addRenderLayer(new Ru9m336MissileLayer(this));
    }

    @Override
    public RenderType getRenderType(Ru9m336MissileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(Ru9m336MissileEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.translate(0, entityIn.getBbHeight() / 2, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
