package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SuperStarProjectileEntity
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import kotlin.math.min

class SuperStarProjectileRenderer(pContext: EntityRendererProvider.Context) :
    EntityRenderer<SuperStarProjectileEntity>(pContext) {
    override fun getBlockLightLevel(pEntity: SuperStarProjectileEntity, pPos: BlockPos): Int {
        return 15
    }

    override fun render(
        pEntity: SuperStarProjectileEntity,
        pEntityYaw: Float,
        pPartialTicks: Float,
        pMatrixStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        pMatrixStack.pushPose()
        pMatrixStack.translate(0.0, min(-0.75 + pEntity.tickCount * 0.05, 0.0), 0.0)
        val viewXRot = mc.gameRenderer.mainCamera.xRot > 0
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation())
        pMatrixStack.mulPose(Axis.XP.rotationDegrees(if (viewXRot) -90f else 90f))
        pMatrixStack.translate(0f, -1f, 0f)
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(pEntity.getLerpTick(pPartialTicks) * (if (viewXRot) 18 else -18)))
        val lastPose = pMatrixStack.last()
        val consumer = pBuffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(pEntity)))
        vertex(consumer, lastPose, pPackedLight, -0.5f, -0.5f, 0, 1)
        vertex(consumer, lastPose, pPackedLight, 0.5f, -0.5f, 1, 1)
        vertex(consumer, lastPose, pPackedLight, 0.5f, 0.5f, 1, 0)
        vertex(consumer, lastPose, pPackedLight, -0.5f, 0.5f, 0, 0)
        pMatrixStack.popPose()
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight)
    }

    override fun getTextureLocation(pEntity: SuperStarProjectileEntity): ResourceLocation {
        return TEXTURE
    }

    companion object {
        private fun vertex(
            pConsumer: VertexConsumer,
            pose: PoseStack.Pose,
            pLightmapUV: Int,
            pX: Float,
            pY: Float,
            pU: Int,
            pV: Int
        ) {
            pConsumer.addVertex(pose, pX, 0f, pY).setColor(255, 255, 0, 255)
                .setUv(pU.toFloat(), pV.toFloat()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(pLightmapUV)
                .setNormal(pose, 0f, 1f, 0f)
        }

        val TEXTURE = loc("textures/particle/white_star.png")
    }
}
