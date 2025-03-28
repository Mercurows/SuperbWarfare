package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
        PoseStack.Pose $$6 = pMatrixStack.last();
        Matrix4f $$7 = $$6.pose();
        Matrix3f $$8 = $$6.normal();
        VertexConsumer $$9 = pBuffer.getBuffer(RenderType.entityCutoutNoCull(texture(pEntity)));
        vertex($$9, $$7, $$8, pPackedLight, 0.0F, 0, 0, 1);
        vertex($$9, $$7, $$8, pPackedLight, 1.0F, 0, 1, 1);
        vertex($$9, $$7, $$8, pPackedLight, 1.0F, 1, 1, 0);
        vertex($$9, $$7, $$8, pPackedLight, 0.0F, 1, 0, 0);
        pMatrixStack.popPose();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    private static void vertex(VertexConsumer pConsumer, Matrix4f pPose, Matrix3f pNormal, int pLightmapUV, float pX, float pY, int pU, int pV) {
        // TODO vertex
        //        pConsumer.vertex(pPose, pX - 0.5F, pY - 0.25F, 0.0F).color(255, 255, 255, 255).uv((float)pU, (float)pV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pLightmapUV).normal(pNormal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static ResourceLocation texture(Entity entity) {
        return ModUtils.loc("textures/particle/fire_star_" + (entity.tickCount % 8 + 1) + ".png");
    }

    public ResourceLocation getTextureLocation(FlareDecoyEntity pEntity) {
        return texture(pEntity);
    }
}
