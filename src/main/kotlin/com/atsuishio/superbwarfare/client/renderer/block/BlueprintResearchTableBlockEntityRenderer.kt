package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.BlueprintResearchTableBlock
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class BlueprintResearchTableBlockEntityRenderer : BlockEntityRenderer<BlueprintResearchTableBlockEntity> {
    override fun render(
        blockEntity: BlueprintResearchTableBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val model = BedrockModelLoader.getModel(BedrockModelLoader.BLUEPRINT_RESEARCH_TABLE_MODEL) ?: return
        val bone = model.getBone("rolling") ?: return

        poseStack.pushPose()

        val rot = when (blockEntity.blockState.getValue(BlueprintResearchTableBlock.FACING)) {
            Direction.EAST -> -90f
            Direction.SOUTH -> 180f
            Direction.WEST -> 90f
            else -> 0f
        }

        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rot))

        if (blockEntity.crafting) {
            bone.rotation.mul(Axis.XP.rotationDegrees(blockEntity.tick * 8 % 360f))
        }

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            packedLight,
            packedOverlay
        )

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.eyes(TEXTURE_E)),
            packedLight,
            packedOverlay
        )

        model.applyPose(model.bindPose)

        poseStack.popPose()
    }

    override fun shouldRender(
        pBlockEntity: BlueprintResearchTableBlockEntity,
        pCameraPos: Vec3
    ): Boolean {
        return pBlockEntity.blockState.getValue(BlueprintResearchTableBlock.PART) == BedPart.FOOT
    }

    override fun getRenderBoundingBox(blockEntity: BlueprintResearchTableBlockEntity): AABB {
        val worldPosition = blockEntity.blockPos

        // 创建一个更大的边界框（示例：覆盖从方块底部到顶部上方2格的范围）
        val expansion = 2.0 // 根据模型实际大小调整
        return AABB(
            (worldPosition.x - 1).toDouble(),
            worldPosition.y.toDouble(),
            (worldPosition.z - 1).toDouble(),
            (worldPosition.x + 2).toDouble(),
            worldPosition.y + expansion,
            (worldPosition.z + 2).toDouble()
        )
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/blueprint_research_table.png")
        val TEXTURE_E = loc("textures/bedrock/block/blueprint_research_table_e.png")
    }
}