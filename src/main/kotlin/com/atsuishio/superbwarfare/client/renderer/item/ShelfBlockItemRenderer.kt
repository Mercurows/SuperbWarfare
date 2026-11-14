package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.renderer.block.ShelfBlockEntityRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class ShelfBlockItemRenderer(dispatcher: BlockEntityRenderDispatcher, models: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, models) {
    override fun renderByItem(
        stack: ItemStack, context: ItemDisplayContext, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        ShelfBlockEntityRenderer.renderModel(ModBlocks.SHELF.get().defaultBlockState(), pose, buffer, light, overlay)
    }
}
