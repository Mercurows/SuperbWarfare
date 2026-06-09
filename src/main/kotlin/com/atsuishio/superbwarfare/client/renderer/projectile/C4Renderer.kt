package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf

class C4Renderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<C4Entity>(renderManager) {
    init {
        this.shadowRadius = 0f
    }

    override fun render(
        entityIn: C4Entity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()
        poseStack.rotateAround(
            Quaternionf(entityIn.getQuaternion(partialTicks)),
            0f, 0f, 0f
        )

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

    override fun getTextureLocation(entity: C4Entity): ResourceLocation {
        val uuid = entity.getUUID()
        return if (uuid.leastSignificantBits % 114 == 0L) {
            TEXTURE_ALTER
        } else {
            TEXTURE
        }
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/c4.png")
        val TEXTURE_ALTER = loc("textures/bedrock/projectile/c4_alter.png")
        val MODEL = loc("models/bedrock/projectile/c4.geo.json")
    }
}
