package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.BedrockModelLoader
import com.atsuishio.superbwarfare.client.BedrockModelLoader.getModel
import com.atsuishio.superbwarfare.entity.SenpaiEntity
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class SenpaiRenderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<SenpaiEntity>(renderManager) {
    init {
        this.shadowRadius = 0.5f
    }

    override fun getTextureLocation(pEntity: SenpaiEntity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        pEntity: SenpaiEntity,
        pEntityYaw: Float,
        pPartialTick: Float,
        pPoseStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int
    ) {
        val model = getModel(BedrockModelLoader.SENPAI_MODEL)
        if (model != null) {
            pPoseStack.pushPose()
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180f))
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-pEntity.getViewYRot(pPartialTick)))

            val renderType = RenderType.entityCutout(getTextureLocation(pEntity))
            val vertexConsumer = pBuffer.getBuffer(renderType)

            val ani = pEntity.animationInstance
            ani.context.partialTick = pPartialTick
            ani.tick()
            model.applyPose(BLENDER.blend(model.bindPose, ani.pose))

            model.renderToBuffer(
                pPoseStack,
                vertexConsumer,
                pPackedLight,
                OverlayTexture.pack(0f, pEntity.hurtTime > 0 || pEntity.deathTime > 0)
            )
            pPoseStack.popPose()
        }
    }

    companion object {
        var TEXTURE = loc("textures/entity/senpai.png")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
