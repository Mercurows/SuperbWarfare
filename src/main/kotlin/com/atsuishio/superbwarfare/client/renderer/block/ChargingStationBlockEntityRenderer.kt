package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.ChargingStationBlock
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class ChargingStationBlockEntityRenderer : BlockEntityRenderer<ChargingStationBlockEntity> {
    override fun render(
        pBlockEntity: ChargingStationBlockEntity,
        pPartialTick: Float,
        pPoseStack: PoseStack,
        pBuffer: MultiBufferSource,
        pPackedLight: Int,
        pPackedOverlay: Int
    ) {
        if (!pBlockEntity.blockState.getValue(ChargingStationBlock.SHOW_RANGE)) return

        pPoseStack.pushPose()
        val pos = pBlockEntity.blockPos
        pPoseStack.translate(-pos.x.toFloat(), -pos.y.toFloat(), -pos.z.toFloat())

        val aabb = AABB(pos).inflate(ChargingStationBlockEntity.CHARGE_RADIUS.toDouble())

        val startX = aabb.minX.toFloat() - 0.001f
        val startY = aabb.minY.toFloat() - 0.001f
        val startZ = aabb.minZ.toFloat() - 0.001f
        val endX = aabb.maxX.toFloat() + 0.001f
        val endY = aabb.maxY.toFloat() + 0.001f
        val endZ = aabb.maxZ.toFloat() + 0.001f

        val red = 0.0f
        val green = 1.0f
        val blue = 0.0f
        val alpha = 0.2f


        val builder = pBuffer.getBuffer(ModRenderTypes.BLOCK_OVERLAY)
        val m4f = pPoseStack.last().pose()

        // east
        builder.vertex(m4f, startX, startY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, startZ).color(red, green, blue, alpha).endVertex()

        // west
        builder.vertex(m4f, startX, startY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red, green, blue, alpha).endVertex()

        // south
        builder.vertex(m4f, endX, startY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red, green, blue, alpha).endVertex()

        // north
        builder.vertex(m4f, startX, startY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, startY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, startZ).color(red, green, blue, alpha).endVertex()

        // top
        builder.vertex(m4f, startX, endY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red, green, blue, alpha).endVertex()

        // bottom
        builder.vertex(m4f, startX, startY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, startZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red, green, blue, alpha).endVertex()
        builder.vertex(m4f, startX, startY, endZ).color(red, green, blue, alpha).endVertex()


        pPoseStack.popPose()
    }

    override fun shouldRenderOffScreen(pBlockEntity: ChargingStationBlockEntity): Boolean {
        return true
    }

    override fun shouldRender(pBlockEntity: ChargingStationBlockEntity, pCameraPos: Vec3): Boolean {
        return true
    }
}
