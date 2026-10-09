package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.LungeMineModel
import com.atsuishio.superbwarfare.item.LungeMineItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

open class LungeMineRenderer : GeoItemRenderer<LungeMineItem>(LungeMineModel()) {
    override fun getRenderType(
        animatable: LungeMineItem?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}

