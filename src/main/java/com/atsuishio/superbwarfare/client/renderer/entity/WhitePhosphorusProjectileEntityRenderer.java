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
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(pEntity)));
        vertex($$9, $$6, pPackedLight, 0, 0, 0, 1);
        vertex($$9, $$6, pPackedLight, 1, 0, 1, 1);
        vertex($$9, $$6, pPackedLight, 1, 1, 1, 0);
        vertex($$9, $$6, pPackedLight, 0, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pose, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.addVertex(pose, pX - 0.5F, pY - 0.25F, 0).setColor(255, 255, 255, 255).setUv((float) pU, (float) pV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(pLightmapUV).setNormal(pose, 0F, 1F, 0F);
    }

    private static ResourceLocation texture(Entity entity) {
        return Mod.loc("textures/particle/fire_star_" + (entity.tickCount % 8 + 1) + ".png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull WhitePhosphorusProjectileEntity pEntity) {
        return texture(pEntity);
    }
}
