package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.LuckyContainerItemModel
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

class LuckyContainerBlockItemRenderer : GeoItemRenderer<LuckyContainerBlockItem>(LuckyContainerItemModel()) {
    override fun getRenderType(
        animatable: LuckyContainerBlockItem,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
