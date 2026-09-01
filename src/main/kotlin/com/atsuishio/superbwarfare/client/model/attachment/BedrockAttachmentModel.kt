package com.atsuishio.superbwarfare.client.model.attachment

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

class BedrockAttachmentModel(private val baseModel: TreeBedrockModel) {
    private val instance: TreeModelInstance = baseModel.createInstance()

    fun getGlobalTransform(boneName: String): Matrix4f? {
        val index = baseModel.getIndex(boneName)
        return if (index >= 0) instance.getGlobalTransform(index) else null
    }

    fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        packedOverlay: Int
    ) {
        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            RenderType.entityCutout(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            packedOverlay,
            1f,
            1f,
            1f,
            1f,
            true
        )
    }
}
