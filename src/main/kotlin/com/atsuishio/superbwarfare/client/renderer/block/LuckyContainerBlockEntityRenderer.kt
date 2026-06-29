package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.LuckyContainerBlock
import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import com.atsuishio.superbwarfare.client.animation.block.LuckyContainerBlockAnimationInstance
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction

class LuckyContainerBlockEntityRenderer : BlockEntityRenderer<LuckyContainerBlockEntity> {
    override fun render(
        blockEntity: LuckyContainerBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val model = BlockModelReloadListener.getModel(MODEL) ?: return
        if (blockEntity.animationInstance == null) {
            blockEntity.animationInstance = LuckyContainerBlockAnimationInstance(blockEntity)
        }
        val ani = blockEntity.animationInstance ?: return

        poseStack.pushPose()

        val rot = when (blockEntity.blockState.getValue(LuckyContainerBlock.FACING)) {
            Direction.EAST -> -90f
            Direction.SOUTH -> 180f
            Direction.WEST -> 90f
            else -> 0f
        }

        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rot))

        ani.context.partialTick = partialTick
        ani.tick()
        model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            packedLight,
            packedOverlay
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/lucky_container.png")
        val MODEL = loc("models/bedrock/block/lucky_container.geo.json")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
