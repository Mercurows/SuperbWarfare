package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.WheelChairModel;
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WheelChairRenderer extends GeoEntityRenderer<WheelChairEntity> {

    public WheelChairRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WheelChairModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(WheelChairEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, WheelChairEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(WheelChairEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        Vec3 root = new Vec3(0, 0.4, 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.getRoll())), (float) root.x, (float) root.y, (float) root.z);
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, WheelChairEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();
        if (name.equals("w_rb")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("w_lb")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        if (name.equals("w_rr")) {
            bone.setRotX(4 * Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("w_lr")) {
            bone.setRotX(4 * Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
