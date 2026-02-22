package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.BlueprintResearchTableBlockItemModel
import com.atsuishio.superbwarfare.item.BlueprintResearchTableBlockItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

// TODO 把手持模型的位置和大小调一下
class BlueprintResearchingTableBlockItemRenderer : GeoItemRenderer<BlueprintResearchTableBlockItem>(
    BlueprintResearchTableBlockItemModel()
) {
    override fun getRenderType(
        animatable: BlueprintResearchTableBlockItem?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(getTextureLocation(animatable))
}