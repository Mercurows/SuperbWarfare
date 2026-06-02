package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.Blu43Entity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class Blu43Renderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<Blu43Entity>(renderManager) {
    override fun getTextureLocation(pEntity: Blu43Entity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        entityIn: Blu43Entity,
        entityYaw: Float,
        pPartialTick: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()

        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw + 180f))

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

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/blu_43.png")
        val MODEL = loc("models/bedrock/projectile/blu_43.geo.json")
    }
}
