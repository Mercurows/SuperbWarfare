package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.resource.BedrockModelLoader.getModel
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
import net.minecraft.util.Mth

class Ptkm1rRenderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<Ptkm1rEntity>(renderManager) {
    override fun shouldShowName(pEntity: Ptkm1rEntity): Boolean {
        return false
    }

    override fun getTextureLocation(pEntity: Ptkm1rEntity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        entity: Ptkm1rEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val model = getModel(BedrockModelLoader.PTKM_1R_MA.first) ?: return
        val ani = entity.animationInstance ?: return

        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTick) + 180f))

        val renderType = RenderType.entityTranslucent(getTextureLocation(entity))
        val vertexConsumer = buffer.getBuffer(renderType)

        ani.context.partialTick = partialTick
        ani.tick()
        model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))

        // TODO 把这个轴调了
        val bodyBone = model.getBone("body")
        if (bodyBone != null) {
            bodyBone.rotation.mul(Axis.YP.rotationDegrees(-entity.yRot))
        }

        val zhuBone = model.getBone("zhu2")
        if (zhuBone != null) {
            zhuBone.rotation.mul(
                Axis.XP.rotation(
                    0.5f * Mth.lerp(
                        partialTick,
                        entity.xRotO,
                        entity.xRot
                    )
                )
            )
        }

        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/ptkm_1r.png")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
