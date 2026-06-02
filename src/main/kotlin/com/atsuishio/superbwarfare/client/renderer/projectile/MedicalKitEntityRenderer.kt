package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.MedicalKitEntity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

class MedicalKitEntityRenderer(renderManager: EntityRendererProvider.Context) :
    EntityRenderer<MedicalKitEntity>(renderManager) {
    init {
        this.shadowRadius = 0f
    }

    override fun shouldShowName(pEntity: MedicalKitEntity): Boolean {
        return false
    }

    override fun render(
        entityIn: MedicalKitEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()
        if (entityIn.deltaMovement.lengthSqr() > 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw + 180f))
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.xRot) + 90))
        }

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

    override fun getTextureLocation(pEntity: MedicalKitEntity): ResourceLocation {
        return TEXTURE
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/medical_kit.png")
        val MODEL = loc("models/bedrock/projectile/medical_kit.geo.json")
    }
}
