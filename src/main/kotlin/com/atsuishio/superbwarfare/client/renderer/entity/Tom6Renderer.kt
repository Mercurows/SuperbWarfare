package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.TntMinecartRenderer
import net.minecraft.world.level.block.Blocks

class Tom6Renderer(manager: EntityRendererProvider.Context) : SbmVehicleRenderer<Tom6Entity>(manager) {

    private val blockRenderer: BlockRenderDispatcher = manager.blockRenderDispatcher

    override fun renderCustomPart(
        vehicle: Tom6Entity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        if (vehicle.hasMelon) {
            poseStack.pushPose()
            poseStack.scale(0.85f, 0.85f, 0.85f)

            poseStack.pushPose()
            poseStack.translate(0.0, 0.7, 0.0)
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f))
            poseStack.translate(-0.5, -0.5, 0.5)
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f))
            TntMinecartRenderer.renderWhiteSolidBlock(
                this.blockRenderer,
                Blocks.MELON.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                false
            )
            poseStack.popPose()
            poseStack.popPose()
        }
    }
}
