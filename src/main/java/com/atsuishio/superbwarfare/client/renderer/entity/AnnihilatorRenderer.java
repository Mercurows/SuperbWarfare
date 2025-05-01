package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.*;
import com.atsuishio.superbwarfare.client.model.entity.AnnihilatorModel;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AnnihilatorRenderer extends GeoEntityRenderer<AnnihilatorEntity> {

    public AnnihilatorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnnihilatorModel());
        this.addRenderLayer(new AnnihilatorLayer(this));
        this.addRenderLayer(new AnnihilatorGlowLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLightLayer(this));
        this.addRenderLayer(new AnnihilatorLedLayer(this));
        this.addRenderLayer(new AnnihilatorLedLightLayer(this));
    }

    @Override
    public RenderType getRenderType(AnnihilatorEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, AnnihilatorEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(AnnihilatorEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AnnihilatorEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        Minecraft minecraft = Minecraft.getInstance();
        Frustum pCamera = minecraft.levelRenderer.getFrustum();

        AABB aabb = animatable.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = new AABB(animatable.getX() - 6.0, animatable.getY() - 4.0, animatable.getZ() - 6.0, animatable.getX() + 6.0, animatable.getY() + 4.0, animatable.getZ() + 6.0);
        }

        if (name.equals("bone")) {
            bone.setHidden(!pCamera.isVisible(aabb));
        }

        if (name.equals("main") || name.equals("main2")) {
            bone.setRotY(-Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) * Mth.DEG_TO_RAD);
        }

        if (name.equals("PaoGuan") || name.equals("PaoGuan2")) {
            bone.setRotX(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
