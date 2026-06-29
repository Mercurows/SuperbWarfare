package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.SmallContainerBlock
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity
import com.atsuishio.superbwarfare.client.animation.block.SmallContainerBlockAnimationInstance
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

class SmallContainerBlockEntityRenderer : BlockEntityRenderer<SmallContainerBlockEntity> {
    override fun render(
        blockEntity: SmallContainerBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val model = BlockModelReloadListener.getModel(MODEL) ?: return
        if (blockEntity.animationInstance == null) {
            blockEntity.animationInstance = SmallContainerBlockAnimationInstance(blockEntity)
        }
        val ani = blockEntity.animationInstance ?: return

        poseStack.pushPose()

        val rot = when (blockEntity.blockState.getValue(SmallContainerBlock.FACING)) {
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

        val texture = if (blockEntity.lootTableSeed != 0L && blockEntity.lootTableSeed % 205 == 0L) {
            TEXTURE_SUI
        } else {
            TEXTURE
        }

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(texture)),
            packedLight,
            packedOverlay
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/small_container.png")
        val TEXTURE_SUI = loc("textures/bedrock/block/small_container_sui.png")
        val MODEL = loc("models/bedrock/block/small_container.geo.json")
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
