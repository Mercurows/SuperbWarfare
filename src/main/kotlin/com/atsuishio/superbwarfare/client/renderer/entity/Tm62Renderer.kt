package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Tm62Entity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

class Tm62Renderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<Tm62Entity>(renderManager) {
    override fun render(
        entityIn: Tm62Entity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = BedrockModelLoader.getModel(BedrockModelLoader.TM_62_MODEL) ?: return

        poseStack.pushPose()

        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.yRot) + 180f))

        val renderType = RenderType.entityTranslucent(getTextureLocation(entityIn))
        val vertexConsumer = bufferIn.getBuffer(renderType)

        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            packedLightIn,
            OverlayTexture.NO_OVERLAY
        )

        poseStack.popPose()
    }

    override fun getTextureLocation(pEntity: Tm62Entity): ResourceLocation {
        return TEXTURE
    }

    override fun shouldShowName(pEntity: Tm62Entity): Boolean {
        return false
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/tm_62.png")
    }
}
