package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.PrismaticBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class PrismaticBoltEntityRenderer extends EntityRenderer<PrismaticBoltEntity> {
    public PrismaticBoltEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @ParametersAreNonnullByDefault
    protected int getBlockLightLevel(PrismaticBoltEntity pEntity, BlockPos pPos) {
        return 15;
    }

    public void render(@NotNull PrismaticBoltEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180F));
        pMatrixStack.rotateAround(Axis.ZP.rotationDegrees(pEntity.randomAngle), 0, 0, 0);
        PoseStack.Pose $$6 = pMatrixStack.last();
        float lerpTick = pEntity.getLerpTick(pPartialTicks);
        float ySpeed = 5 - 2.5f * lerpTick;
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityTranslucentEmissive(texture()));
        vertex($$9, $$6, pPackedLight, -1.5f, -1.5f + ySpeed, 0, 1);
        vertex($$9, $$6, pPackedLight, 1.5f, -1.5f + ySpeed, 1, 1);
        vertex($$9, $$6, pPackedLight, 1.5f, 1.5f + ySpeed, 1, 0);
        vertex($$9, $$6, pPackedLight, -1.5f, 1.5f + ySpeed, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldRender(PrismaticBoltEntity pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.addVertex(pPose, pX, pY, 0)
                .setColor(255, 255, 255, 255)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0F, 1F, 0F);
    }

    private static ResourceLocation texture() {
        return Mod.loc("textures/particle/prismatic_bolt.png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull PrismaticBoltEntity pEntity) {
        return texture();
    }
}
