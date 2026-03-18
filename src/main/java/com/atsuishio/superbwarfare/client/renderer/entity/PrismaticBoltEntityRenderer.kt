package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.PrismaticBoltEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation

class PrismaticBoltEntityRenderer(pContext: EntityRendererProvider.Context) :
    EntityRenderer<PrismaticBoltEntity>(pContext) {
    override fun getBlockLightLevel(pEntity: PrismaticBoltEntity, pPos: BlockPos): Int {
        return 15
    }

    override fun render(
        pEntity: PrismaticBoltEntity,
        pEntityYaw: Float,
        pPartialTicks: Float,
        pMatrixStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        pMatrixStack.pushPose()
        pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation())
        pMatrixStack.mulPose(Axis.YP.rotationDegrees(180f))
        pMatrixStack.rotateAround(Axis.ZP.rotationDegrees(pEntity.randomAngle), 0f, 0f, 0f)
        val lastPose = pMatrixStack.last()
        val lerpTick = pEntity.getLerpTick(pPartialTicks)
        val ySpeed = 5 - 2.5f * lerpTick
        val consumer = pBuffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE))
        vertex(consumer, lastPose, pPackedLight, -1.5f, -1.5f + ySpeed, 0, 1)
        vertex(consumer, lastPose, pPackedLight, 1.5f, -1.5f + ySpeed, 1, 1)
        vertex(consumer, lastPose, pPackedLight, 1.5f, 1.5f + ySpeed, 1, 0)
        vertex(consumer, lastPose, pPackedLight, -1.5f, 1.5f + ySpeed, 0, 0)
        pMatrixStack.popPose()
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight)
    }

    override fun shouldRender(
        pLivingEntity: PrismaticBoltEntity,
        pCamera: Frustum,
        pCamX: Double,
        pCamY: Double,
        pCamZ: Double
    ): Boolean {
        return true
    }

    override fun getTextureLocation(pEntity: PrismaticBoltEntity): ResourceLocation {
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
            pConsumer.addVertex(pPose, pX, pY, 0f)
                .setColor(255, 255, 255, 255)
                .setUv(pU.toFloat(), pV.toFloat())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0f, 1f, 0f)
        }

        val TEXTURE = loc("textures/particle/prismatic_bolt.png")
    }
}
