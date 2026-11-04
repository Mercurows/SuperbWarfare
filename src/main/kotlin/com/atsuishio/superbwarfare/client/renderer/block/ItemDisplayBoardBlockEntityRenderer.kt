package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.ItemDisplayBoardBlock
import com.atsuishio.superbwarfare.block.entity.ItemDisplayBoardBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext

class ItemDisplayBoardBlockEntityRenderer : BlockEntityRenderer<ItemDisplayBoardBlockEntity> {
    override fun render(
        blockEntity: ItemDisplayBoardBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val stack = blockEntity.item
        if (stack.isEmpty) return

        val state = blockEntity.blockState
        val facing = state.getValue(ItemDisplayBoardBlock.FACING)
        val rotation = state.getValue(ItemDisplayBoardBlock.ROTATION)

        poseStack.pushPose()
        // 板体固定在朝向相反一侧（贴墙/贴地），物品位于板面前方 1px
        poseStack.translate(
            0.5 - facing.stepX * 0.375,
            0.5 - facing.stepY * 0.375,
            0.5 - facing.stepZ * 0.375
        )
        when (facing) {
            Direction.UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f))
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }

            Direction.DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90f))
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }

            else -> poseStack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()))
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 45f))
        poseStack.scale(0.5f, 0.5f, 0.5f)

        Minecraft.getInstance().itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            blockEntity.level,
            0
        )

        poseStack.popPose()
    }
}
