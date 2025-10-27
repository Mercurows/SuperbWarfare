package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerGlowLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerLaserLayer;
import com.atsuishio.superbwarfare.client.model.entity.WaveforceTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;

public class WaveforceTowerRenderer extends VehicleRenderer<WaveforceTowerEntity> {

    public WaveforceTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WaveforceTowerModel());
        this.addRenderLayer(new WaveforceTowerGlowLayer(this));
        this.addRenderLayer(new WaveforceTowerLaserLayer(this));
    }

    @Override
    public void render(WaveforceTowerEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WaveforceTowerEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        Minecraft minecraft = Minecraft.getInstance();
        Frustum pCamera = minecraft.levelRenderer.getFrustum();

        AABB aabb = animatable.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = new AABB(animatable.getX() - 2.0, animatable.getY() - 2.0, animatable.getZ() - 2.0, animatable.getX() + 2.0, animatable.getY() + 2.0, animatable.getZ() + 2.0);
        }

        if (name.equals("root")) {
            bone.setHidden(!pCamera.isVisible(aabb) && !RenderHelper.isInGui());
        }

        if (name.equals("turret") || name.equals("turret2")) {
            bone.setRotY(turretYRot * Mth.DEG_TO_RAD);
        }

        if (name.equals("barrel") || name.equals("barrel2")) {
            bone.setRotX(-turretXRot * Mth.DEG_TO_RAD);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
