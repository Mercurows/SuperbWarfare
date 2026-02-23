package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.SmallContainerItemModel
import com.atsuishio.superbwarfare.item.container.SmallContainerBlockItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

class SmallContainerBlockItemRenderer : GeoItemRenderer<SmallContainerBlockItem>(SmallContainerItemModel()) {
    override fun getRenderType(
        animatable: SmallContainerBlockItem,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
