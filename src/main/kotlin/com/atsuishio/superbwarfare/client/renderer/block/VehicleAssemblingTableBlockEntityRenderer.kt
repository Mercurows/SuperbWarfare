package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

class VehicleAssemblingTableBlockEntityRenderer : BlockEntityRenderer<VehicleAssemblingTableBlockEntity> {
    override fun render(
        blockEntity: VehicleAssemblingTableBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val model = BlockModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()

        val rot = when (blockEntity.blockState.getValue(VehicleAssemblingTableBlock.FACING)) {
            Direction.EAST -> -90f
            Direction.SOUTH -> 180f
            Direction.WEST -> 90f
            else -> 0f
        }

        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rot))

        model.applyPose(model.bindPose)

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            packedLight,
            packedOverlay
        )

        if (blockEntity.level?.isNight == true) {
            model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.eyes(TEXTURE_E)),
                packedLight,
                packedOverlay
            )
        }

        poseStack.popPose()
    }

    override fun shouldRender(blockEntity: VehicleAssemblingTableBlockEntity, cameraPos: Vec3): Boolean {
        return blockEntity.blockState.getValue(VehicleAssemblingTableBlock.BLOCK_PART) == BlockPart.FLB
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/vehicle_assembling_table.png")
        val TEXTURE_E = loc("textures/bedrock/block/vehicle_assembling_table_e.png")
        val MODEL = loc("models/bedrock/block/vehicle_assembling_table.geo.json")
    }
}
