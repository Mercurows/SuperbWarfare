package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.misc.HighEnergyExplosivesEntity
import com.atsuishio.superbwarfare.init.ModBlocks
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.TntMinecartRenderer
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.resources.ResourceLocation

class HighEnergyExplosivesRenderer(context: EntityRendererProvider.Context) : EntityRenderer<HighEnergyExplosivesEntity>(context) {
    private val blockRenderer = context.blockRenderDispatcher

    init {
        shadowRadius = 0.5f
    }

    override fun render(entity: HighEnergyExplosivesEntity, yaw: Float, partialTick: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int) {
        pose.pushPose()
        pose.translate(0.0, 0.5, 0.0)
        val remaining = entity.fuse - partialTick + 1f
        if (remaining < 10f) {
            val progress = (1f - remaining / 10f).coerceIn(0f, 1f)
            val scale = 1f + progress * progress * progress * progress * 0.3f
            pose.scale(scale, scale, scale)
        }
        pose.translate(-0.5, -0.5, -0.5)
        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderer, ModBlocks.HIGH_ENERGY_EXPLOSIVES_BLOCK.get().defaultBlockState(),
            pose, buffers, light, entity.fuse / 5 % 2 == 0)
        pose.popPose()
        super.render(entity, yaw, partialTick, pose, buffers, light)
    }

    override fun getTextureLocation(entity: HighEnergyExplosivesEntity): ResourceLocation = TextureAtlas.LOCATION_BLOCKS
}
