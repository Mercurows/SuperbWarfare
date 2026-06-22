package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity
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
import org.joml.Matrix3f
import org.joml.Matrix4f

class FlareDecoyEntityRenderer(pContext: EntityRendererProvider.Context) :
    EntityRenderer<FlareDecoyEntity>(pContext) {
    override fun getBlockLightLevel(
        pEntity: FlareDecoyEntity,
        pPos: BlockPos
    ): Int {
        return 15
    }

    override fun render(
        pEntity: FlareDecoyEntity,
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
        val pose = lastPose.pose()
        val normal = lastPose.normal()
        val consumer = pBuffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(pEntity)))
        vertex(consumer, pose, normal, pPackedLight, -0.75f, -0.75f, 0, 1)
        vertex(consumer, pose, normal, pPackedLight, 0.75f, -0.75f, 1, 1)
        vertex(consumer, pose, normal, pPackedLight, 0.75f, 0.75f, 1, 0)
        vertex(consumer, pose, normal, pPackedLight, -0.75f, 0.75f, 0, 0)
        pMatrixStack.popPose()
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight)
    }

    override fun getTextureLocation(entity: FlareDecoyEntity): ResourceLocation {
        return TEXTURES[entity.tickCount % 8]
    }

    companion object {
        private fun vertex(
            pConsumer: VertexConsumer,
            pPose: Matrix4f,
            pNormal: Matrix3f,
            pLightmapUV: Int,
            pX: Float,
            pY: Float,
            pU: Int,
            pV: Int
        ) {
            pConsumer.vertex(pPose, pX - 0.5f, pY - 0.25f, 0f).color(255, 255, 255, 255).uv(pU.toFloat(), pV.toFloat())
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pLightmapUV).normal(pNormal, 0f, 1f, 0f).endVertex()
        }

        val TEXTURES: List<ResourceLocation> = ArrayList((0..7).map { loc("textures/particle/fire_star_$it.png") })
    }
}
