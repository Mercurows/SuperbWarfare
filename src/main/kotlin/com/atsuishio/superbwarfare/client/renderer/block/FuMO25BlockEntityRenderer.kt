package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.util.Mth

class FuMO25BlockEntityRenderer : BlockEntityRenderer<FuMO25BlockEntity> {
    override fun render(
        blockEntity: FuMO25BlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val model = BedrockModelLoader.getModel(BedrockModelLoader.FUMO_25_MODEL) ?: return
        val bone = model.getBone("rolling") ?: return

        poseStack.pushPose()

        poseStack.translate(0.5, 0.0, 0.5)

        bone.rotation.mul(
            Axis.YN.rotationDegrees(
                Mth.lerp(
                    partialTick,
                    blockEntity.tickO.toFloat(),
                    blockEntity.tick.toFloat()
                )
            )
        )

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            packedLight,
            packedOverlay
        )

        model.applyPose(model.bindPose)

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/fumo_25.png")
    }

    override fun getViewDistance(): Int {
        return 256
    }
}
