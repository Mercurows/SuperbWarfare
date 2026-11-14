package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.misc.FoodRainItemEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.ItemEntityRenderer
import net.minecraft.world.entity.item.ItemEntity

class FoodRainItemRenderer(context: EntityRendererProvider.Context) : ItemEntityRenderer(context) {
    init {
        shadowRadius *= 8f
    }

    override fun render(
        entity: ItemEntity, yaw: Float, partialTick: Float, pose: PoseStack,
        buffers: MultiBufferSource, light: Int
    ) {
        if ((entity as? FoodRainItemEntity)?.hidden == true) return
        pose.pushPose()
        pose.scale(8f, 8f, 8f)
        super.render(entity, yaw, partialTick, pose, buffers, light)
        pose.popPose()
    }
}
