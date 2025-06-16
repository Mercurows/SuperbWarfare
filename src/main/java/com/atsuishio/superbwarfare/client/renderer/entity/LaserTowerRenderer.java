package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerLaserLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerPowerLayer;
import com.atsuishio.superbwarfare.client.model.entity.LaserTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
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
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LaserTowerRenderer extends GeoEntityRenderer<LaserTowerEntity> {

    public LaserTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LaserTowerModel());
        this.addRenderLayer(new LaserTowerPowerLayer(this));
        this.addRenderLayer(new LaserTowerLaserLayer(this));
    }

    @Override
    public RenderType getRenderType(LaserTowerEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, LaserTowerEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(@NotNull LaserTowerEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, LaserTowerEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        Minecraft minecraft = Minecraft.getInstance();
        Frustum pCamera = minecraft.levelRenderer.getFrustum();

        AABB aabb = animatable.getBoundingBoxForCulling().inflate(0.5);
        if (aabb.hasNaN() || aabb.getSize() == 0.0) {
            aabb = new AABB(animatable.getX() - 2.0, animatable.getY() - 2.0, animatable.getZ() - 2.0, animatable.getX() + 2.0, animatable.getY() + 2.0, animatable.getZ() + 2.0);
        }

        if (name.equals("root")) {
            bone.setHidden(!pCamera.isVisible(aabb));
        }

        if (name.equals("turret") || name.equals("turret2")) {
            bone.setRotY(-Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) * Mth.DEG_TO_RAD);
        }

        if (name.equals("barrel") || name.equals("barrel2")) {
            bone.setRotX(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
