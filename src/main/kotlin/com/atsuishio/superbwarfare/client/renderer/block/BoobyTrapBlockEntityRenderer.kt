package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.block.entity.BoobyTrapBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer

class BoobyTrapBlockEntityRenderer : BlockEntityRenderer<BoobyTrapBlockEntity> {
    override fun render(entity: BoobyTrapBlockEntity, partialTick: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        if (!entity.blockState.getValue(BoobyTrapBlock.CAMOUFLAGED)) return
        val state = entity.camouflage ?: return
        Minecraft.getInstance().blockRenderer.renderSingleBlock(state, pose, buffers, light, overlay)
    }
}
