package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SuperStarProjectileEntity;
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

import javax.annotation.ParametersAreNonnullByDefault;

public class SuperStarProjectileRenderer extends EntityRenderer<SuperStarProjectileEntity> {
    public SuperStarProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @ParametersAreNonnullByDefault
    protected int getBlockLightLevel(SuperStarProjectileEntity pEntity, BlockPos pPos) {
        return 15;
    }

    public void render(@NotNull SuperStarProjectileEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180F));
        pMatrixStack.translate(0, Math.min(-0.5 + pEntity.tickCount * 0.05, 0), -1);
        PoseStack.Pose $$6 = pMatrixStack.last();
        Matrix4f $$7 = $$6.pose();
        Matrix3f $$8 = $$6.normal();
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(pEntity)));
        pMatrixStack.rotateAround(Axis.ZP.rotationDegrees((System.currentTimeMillis() % 36000000) / 2f), 0, 0.125f, 0);
        vertex($$9, $$6, pPackedLight, 0, 0, 0, 1);
        vertex($$9, $$6, pPackedLight, 1, 0, 1, 1);
        vertex($$9, $$6, pPackedLight, 1, 1, 1, 0);
        vertex($$9, $$6, pPackedLight, 0, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int lightmapUV, float x, float y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.25F, 0)
                .setColor(255, 255, 0, 255)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(pose, 0F, 1F, 0F)
                .setLight(lightmapUV);
    }

    private static ResourceLocation texture(Entity entity) {
        return Mod.loc("textures/particle/white_star.png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull SuperStarProjectileEntity pEntity) {
        return texture(pEntity);
    }
}
