package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.ContainerItemModel
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

class ContainerBlockItemRenderer : GeoItemRenderer<ContainerBlockItem>(ContainerItemModel()) {
    override fun getRenderType(
        animatable: ContainerBlockItem,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
