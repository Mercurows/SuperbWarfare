package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.ClaymoreEntity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class ClaymoreRenderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<ClaymoreEntity>(renderManager) {
    init {
        this.shadowRadius = 0f
    }

    override fun render(
        entityIn: ClaymoreEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()

        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw + 180f))
        poseStack.scale(0.5f, 0.5f, 0.5f)

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

    public override fun shouldShowName(animatable: ClaymoreEntity): Boolean {
        return false
    }

    override fun getTextureLocation(pEntity: ClaymoreEntity): ResourceLocation {
        val uuid = pEntity.getUUID()
        return if (uuid.leastSignificantBits % 514 == 0L) {
            TEXTURE_ALTER
        } else {
            TEXTURE
        }
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/claymore.png")
        val TEXTURE_ALTER = loc("textures/bedrock/projectile/claymore_alter.png")
        val MODEL = loc("models/bedrock/projectile/claymore.geo.json")
    }
}
