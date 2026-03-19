package com.atsuishio.superbwarfare.client.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.TntMinecartRenderer
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks

class MelonBombEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity>(context) {
    private val blockRenderer: BlockRenderDispatcher = context.blockRenderDispatcher

    init {
        this.shadowRadius = 0.2f
    }

    override fun render(
        entity: com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity,
        entityYaw: Float,
        partialTicks: Float,
        matrixStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        matrixStack.pushPose()
        matrixStack.translate(0.0, 0.5, 0.0)
        matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0f))
        matrixStack.translate(-0.5, -0.5, 0.5)
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f))
        TntMinecartRenderer.renderWhiteSolidBlock(
            this.blockRenderer,
            Blocks.MELON.defaultBlockState(),
            matrixStack,
            buffer,
            packedLight,
            false
        )
        matrixStack.popPose()
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight)
    }

    override fun getTextureLocation(entity: com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity): ResourceLocation {
        return TextureAtlas.LOCATION_BLOCKS
    }
}
