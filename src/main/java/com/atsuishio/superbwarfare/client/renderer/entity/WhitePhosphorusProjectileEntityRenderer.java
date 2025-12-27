package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.WhitePhosphorusProjectileEntity;
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

public class WhitePhosphorusProjectileEntityRenderer extends EntityRenderer<WhitePhosphorusProjectileEntity> {
    public WhitePhosphorusProjectileEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @ParametersAreNonnullByDefault
    protected int getBlockLightLevel(WhitePhosphorusProjectileEntity pEntity, BlockPos pPos) {
        return 15;
    }

    public void render(@NotNull WhitePhosphorusProjectileEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180F));
        PoseStack.Pose $$6 = pMatrixStack.last();
        Matrix4f $$7 = $$6.pose();
        Matrix3f $$8 = $$6.normal();
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(pEntity)));
        vertex($$9, $$7, $$8, pPackedLight, 0, 0, 0, 1);
        vertex($$9, $$7, $$8, pPackedLight, 1, 0, 1, 1);
        vertex($$9, $$7, $$8, pPackedLight, 1, 1, 1, 0);
        vertex($$9, $$7, $$8, pPackedLight, 0, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer pConsumer, Matrix4f pPose, Matrix3f pNormal, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.vertex(pPose, pX - 0.5F, pY - 0.25F, 0).color(255, 255, 255, 255).uv((float) pU, (float) pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pLightmapUV).normal(pNormal, 0F, 1F, 0F).endVertex();
    }

    private static ResourceLocation texture(Entity entity) {
        return Mod.loc("textures/particle/fire_star_" + (entity.tickCount % 8 + 1) + ".png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull WhitePhosphorusProjectileEntity pEntity) {
        return texture(pEntity);
    }
}
