package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import com.atsuishio.superbwarfare.client.model.block.LuckyContainerBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoBlockRenderer

class LuckyContainerBlockEntityRenderer : GeoBlockRenderer<LuckyContainerBlockEntity>(LuckyContainerBlockModel()) {
    override fun getRenderType(
        animatable: LuckyContainerBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
