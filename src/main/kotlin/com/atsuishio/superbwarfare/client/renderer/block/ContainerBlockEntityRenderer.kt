package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import com.atsuishio.superbwarfare.client.model.block.ContainerBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ContainerBlockEntityRenderer : GeoBlockRenderer<ContainerBlockEntity>(ContainerBlockModel()) {
    override fun getRenderType(
        animatable: ContainerBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityCutout(getTextureLocation(animatable))
    }
}
