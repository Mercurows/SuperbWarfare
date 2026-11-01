package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.ResonanceBaseBlockEntity
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

class ResonanceBaseBlockEntityRenderer : BlockEntityRenderer<ResonanceBaseBlockEntity> {
    private var model = BlockModelReloadListener.getModel(MODEL)
    private var instance = model?.createInstance()

    override fun render(
        blockEntity: ResonanceBaseBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        // Refresh the static instance after resource reload, including for already placed blocks.
        val currentModel = BlockModelReloadListener.getModel(MODEL)
        if (currentModel !== model) {
            model = currentModel
            instance = currentModel?.createInstance()
        }
        val instance = instance ?: return
        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        instance.resetPose()
        instance.renderToBuffer(
            poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay
        )
        poseStack.popPose()
    }

    companion object {
        private val MODEL = loc("models/bedrock/block/resonance_base.geo.json")
        private val TEXTURE = loc("textures/bedrock/block/resonance_base.png")
    }
}
