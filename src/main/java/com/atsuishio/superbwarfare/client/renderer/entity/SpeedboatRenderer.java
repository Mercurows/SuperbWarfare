package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatHeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatPowerLayer;
import com.atsuishio.superbwarfare.client.model.entity.SpeedboatModel;
import com.atsuishio.superbwarfare.entity.WaterMaskEntity;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;

public class SpeedboatRenderer extends VehicleRenderer<SpeedboatEntity> {

    public SpeedboatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpeedboatModel());
        this.addRenderLayer(new SpeedBoatLayer(this));
        this.addRenderLayer(new SpeedBoatPowerLayer(this));
        this.addRenderLayer(new SpeedBoatHeatLayer(this));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SpeedboatEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (bone.getName().equals("waterMask")) {
            super.renderRecursively(poseStack, animatable, bone, RenderType.waterMask(), bufferSource, bufferSource.getBuffer(RenderType.waterMask()), isReRender, partialTick, packedLight, packedOverlay, colour);
        } else {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        }
    }

    @Override
    public void renderCustomPart(SpeedboatEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.scale(2.4f, 0.4f, 4.05f);
        poseStack.translate(0, 1.5, -0.22);
        Entity entity = new WaterMaskEntity(ModEntities.WATER_MASK.get(), entityIn.level());
        entityRenderDispatcher.render(entity, 0, 0, 0, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
