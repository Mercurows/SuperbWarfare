package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.GrapeshotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GrapeshotRenderer extends EntityRenderer<GrapeshotEntity> {
    public GrapeshotRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public void render(@NotNull GrapeshotEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180F));
        PoseStack.Pose $$6 = pMatrixStack.last();
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityTranslucent(texture()));
        vertex($$9, $$6, pPackedLight, 0, 0, 0, 1);
        vertex($$9, $$6, pPackedLight, 1, 0, 1, 1);
        vertex($$9, $$6, pPackedLight, 1, 1, 1, 0);
        vertex($$9, $$6, pPackedLight, 0, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.addVertex(pPose, pX - 0.5F, pY - 0.25F, 0)
                .setColor(255, 255, 255, 255)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0, 1, 0);
    }

    private static ResourceLocation texture() {
        return Mod.loc("textures/entity/grape_projectile.png");
    }

    public @NotNull ResourceLocation getTextureLocation(@NotNull GrapeshotEntity pEntity) {
        return texture();
    }
}
