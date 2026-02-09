package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.DroneLayer;
import com.atsuishio.superbwarfare.client.model.entity.DroneModel;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.*;

public class DroneRenderer extends GeoEntityRenderer<DroneEntity> {
    public DroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DroneModel());
        this.addRenderLayer(new DroneLayer(this));
        this.shadowRadius = 0.2f;
    }

    @Override
    public RenderType getRenderType(DroneEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(DroneEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityIn.getYaw(partialTicks)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entityIn.getBodyPitch(partialTicks)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entityIn.getRoll(partialTicks)));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);

        if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(entityIn.position()) > 0.0625) {
            renderAttachments(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        }

        poseStack.popPose();
    }

    private String entityNameCache = "";
    private Entity entityCache = null;
    private int attachedTick = Integer.MAX_VALUE;

    // 统一渲染挂载实体
    private void renderAttachments(DroneEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var data = entity.getEntityData();
        var attached = data.get(DISPLAY_ENTITY);
        if (attached.isEmpty()) return;

        Entity renderEntity;

        if (entityNameCache.equals(attached) && entityCache != null) {
            renderEntity = entityCache;
        } else {
            renderEntity = EntityType.byString(attached)
                    .map(type -> type.create(entity.level()))
                    .orElse(null);
            if (renderEntity == null) return;

            // 填充tag
            var tag = data.get(DISPLAY_ENTITY_TAG);
            if (!tag.isEmpty()) {
                renderEntity.load(tag);
            }

            entityNameCache = attached;
            entityCache = renderEntity;
            attachedTick = entity.tickCount;
        }
        var displayData = data.get(DISPLAY_DATA);

        renderEntity.tickCount = displayData.get(11) >= 0 ? displayData.get(11).intValue() : entity.tickCount - attachedTick;

        var scale = new float[]{displayData.get(0), displayData.get(1), displayData.get(2)};
        var offset = new float[]{displayData.get(3), displayData.get(4), displayData.get(5)};
        var rotation = new float[]{displayData.get(6), displayData.get(7), displayData.get(8)};
        var xLength = displayData.get(9);
        var yLength = displayData.get(10);

        for (int i = 0; i < animatable.getAmmo(); i++) {
            float x, z;
            if (data.get(MAX_AMMO) == 1) {
                // 神风或单个挂载
                x = 0;
                z = 0;
            } else {
                // 投弹
                x = xLength / 2 * (i % 2 == 0 ? 1 : -1);

                var rows = data.get(MAX_AMMO) / 2;
                var row = i / 2;
                if (rows < 2) {
                    z = 0;
                } else {
                    var rowLength = yLength / rows;
                    z = -yLength / 2 + rowLength * row;
                }
            }

            poseStack.pushPose();
            poseStack.translate(x + offset[0], offset[1], z + offset[2]);
            poseStack.scale(scale[0], scale[1], scale[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation[2]));
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[1]));

            entityRenderDispatcher.render(renderEntity, 0, 0, 0, entityYaw, partialTicks, poseStack, buffer, packedLight);

            poseStack.popPose();
        }
    }
}
