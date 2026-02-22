package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity
import com.atsuishio.superbwarfare.client.model.block.SmallContainerBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoBlockRenderer

class SmallContainerBlockEntityRenderer : GeoBlockRenderer<SmallContainerBlockEntity>(SmallContainerBlockModel()) {
    override fun getRenderType(
        animatable: SmallContainerBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
