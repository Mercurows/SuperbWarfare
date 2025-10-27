package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.RenderHelper;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.GeoBone;

public class LaserTowerRenderer extends VehicleRenderer<LaserTowerEntity> {

    public LaserTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LaserTowerModel());
        this.addRenderLayer(new LaserTowerPowerLayer(this));
        this.addRenderLayer(new LaserTowerLaserLayer(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, LaserTowerEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
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

        if (name.startsWith("turret")) {
            bone.setRotY(-Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) * Mth.DEG_TO_RAD);
        }

        if (name.startsWith("barrel")) {
            bone.setRotX(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
