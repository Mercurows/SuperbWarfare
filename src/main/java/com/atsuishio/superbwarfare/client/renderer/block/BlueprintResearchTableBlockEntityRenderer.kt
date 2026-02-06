package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.client.model.block.BlueprintResearchTableBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoBlockRenderer

class BlueprintResearchTableBlockEntityRenderer : GeoBlockRenderer<BlueprintResearchTableBlockEntity>(
    BlueprintResearchTableBlockModel()
) {
    override fun getRenderType(
        animatable: BlueprintResearchTableBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(getTextureLocation(animatable))
}