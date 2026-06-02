package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.SteelCoilEntity
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class SteelCoilRenderer(renderManager: EntityRendererProvider.Context) :
    EntityRenderer<SteelCoilEntity>(renderManager) {

    override fun render(
        entity: SteelCoilEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val model = EntityModelReloadListener.getModel(MODEL) ?: return
        val bone = model.getBone("main") ?: return

        poseStack.pushPose()

        poseStack.scale(2.0f, 2.0f, 2.0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw + 180f))

        bone.rotation.mul(Axis.XP.rotationDegrees(-entity.getRotation(partialTick)))

        model.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityCutout(getTextureLocation(entity)),
            BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(entity)),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        model.applyPose(model.bindPose)

        poseStack.popPose()
    }

    override fun getTextureLocation(entity: SteelCoilEntity): ResourceLocation {
        return if (entity.uuid.leastSignificantBits % 810 == 0L) TEXTURE_ALTER else TEXTURE
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/entity/steel_coil.png")
        val TEXTURE_ALTER = loc("textures/bedrock/entity/steel_coil_alter.png")
        val MODEL = loc("models/bedrock/entity/steel_coil.geo.json")
    }
}