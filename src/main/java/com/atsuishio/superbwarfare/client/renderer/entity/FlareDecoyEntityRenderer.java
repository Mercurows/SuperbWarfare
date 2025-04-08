package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class FlareDecoyEntityRenderer extends EntityRenderer<FlareDecoyEntity> {
    public FlareDecoyEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    protected int getBlockLightLevel(@NotNull FlareDecoyEntity pEntity, @NotNull BlockPos pPos) {
        return 15;
    }

    public void render(@NotNull FlareDecoyEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.scale(1.0F, 1.0F, 1.0F);
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = pMatrixStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(pEntity)));
        vertex(consumer, matrix, pose, normal, pPackedLight, 0.0F, 0, 0, 1);
        vertex(consumer, matrix, pose, normal, pPackedLight, 1.0F, 0, 1, 1);
        vertex(consumer, matrix, pose, normal, pPackedLight, 1.0F, 1, 1, 0);
        vertex(consumer, matrix, pose, normal, pPackedLight, 0.0F, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose, Matrix3f normal, int lightmapUV, float pX, float pY, int pU, int pV) {
        consumer.addVertex(matrix, pX - 0.5F, pY - 0.25F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                .setLight(lightmapUV);
    }

    private static ResourceLocation texture(Entity entity) {
        return Mod.loc("textures/particle/fire_star_" + (entity.tickCount % 8 + 1) + ".png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull FlareDecoyEntity pEntity) {
        return texture(pEntity);
    }
}
