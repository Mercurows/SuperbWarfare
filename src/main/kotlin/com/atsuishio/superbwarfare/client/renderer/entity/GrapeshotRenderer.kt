package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.GrapeshotEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class GrapeshotRenderer(pContext: EntityRendererProvider.Context) : EntityRenderer<GrapeshotEntity>(pContext) {
    override fun render(
        pEntity: GrapeshotEntity,
        pEntityYaw: Float,
        pPartialTicks: Float,
        pMatrixStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        pMatrixStack.pushPose()
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation())
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180f))
        val lastPose = pMatrixStack.last()
        val consumer = pBuffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(pEntity)))
        vertex(consumer, lastPose, pPackedLight, 0f, 0f, 0, 1)
        vertex(consumer, lastPose, pPackedLight, 1f, 0f, 1, 1)
        vertex(consumer, lastPose, pPackedLight, 1f, 1f, 1, 0)
        vertex(consumer, lastPose, pPackedLight, 0f, 1f, 0, 0)
        pMatrixStack.popPose()
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight)
    }

    override fun getTextureLocation(pEntity: GrapeshotEntity): ResourceLocation {
        return TEXTURE
    }

    companion object {
        private fun vertex(
            pConsumer: VertexConsumer,
            pPose: PoseStack.Pose,
            pLightmapUV: Int,
            pX: Float,
            pY: Float,
            pU: Int,
            pV: Int
        ) {
            pConsumer.addVertex(pPose, pX - 0.5f, pY - 0.25f, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(pU.toFloat(), pV.toFloat())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0f, 1f, 0f)
        }

        val TEXTURE = loc("textures/entity/grape_projectile.png")
    }
}
