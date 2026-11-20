package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.misc.FoodRainItemEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.ItemEntityRenderer
import net.minecraft.world.entity.item.ItemEntity

class FoodRainItemRenderer(context: EntityRendererProvider.Context) : ItemEntityRenderer(context) {
    init {
        shadowRadius *= 8f
    }

    override fun shouldRender(entity: ItemEntity, frustum: Frustum, x: Double, y: Double, z: Double): Boolean {
        return (entity as? FoodRainItemEntity)?.hidden != true && super.shouldRender(entity, frustum, x, y, z)
    }

    override fun render(
        entity: ItemEntity, yaw: Float, partialTick: Float, pose: PoseStack,
        buffers: MultiBufferSource, light: Int
    ) {
        if ((entity as? FoodRainItemEntity)?.hidden == true) return
        pose.pushPose()
        try {
            pose.scale(8f, 8f, 8f)
            super.render(entity, yaw, partialTick, pose, buffers, light)
        } finally {
            pose.popPose()
        }
    }
}
