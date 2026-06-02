package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.BlueprintResearchTableBlock
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BedPart
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
        val model = BlockModelReloadListener.getModel(MODEL) ?: return
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

    companion object {
        val TEXTURE = loc("textures/bedrock/block/blueprint_research_table.png")
        val TEXTURE_E = loc("textures/bedrock/block/blueprint_research_table_e.png")
        val MODEL = loc("models/bedrock/block/blueprint_research_table.geo.json")
    }
}