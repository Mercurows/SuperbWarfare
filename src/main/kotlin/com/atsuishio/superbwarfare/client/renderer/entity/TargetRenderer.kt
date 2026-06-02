package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
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

class TargetRenderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<TargetEntity>(renderManager) {
    override fun getTextureLocation(pEntity: TargetEntity): ResourceLocation {
        return TEXTURE
    }

    override fun render(
        entity: TargetEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val model = EntityModelReloadListener.getModel(MODEL) ?: return
        val ani = entity.animationInstance ?: return

        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(180f))
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getViewYRot(partialTick)))

        val renderType = RenderType.entityTranslucent(getTextureLocation(entity))
        val vertexConsumer = buffer.getBuffer(renderType)

        ani.context.partialTick = partialTick
        ani.tick()
        model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))

        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            packedLight,
            OverlayTexture.pack(0f, entity.hurtTime > 0 || entity.deathTime > 0)
        )

        poseStack.pushPose()
        val bone = model.getBone("ba")
        val boneConsumer = buffer.getBuffer(RenderType.eyes(TEXTURE_E))
        bone.render(poseStack, boneConsumer, packedLight, OverlayTexture.NO_OVERLAY)
        poseStack.popPose()

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/entity/target.png")
        val TEXTURE_E = loc("textures/bedrock/entity/target_e.png")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
        val MODEL = loc("models/bedrock/entity/target.geo.json")
    }
}
