package com.atsuishio.superbwarfare.client.renderer.armor

import com.atsuishio.superbwarfare.client.model.armor.UsHelmetPasgtModel
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgtItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoArmorRenderer

class UsHelmetPasgtArmorRenderer : GeoArmorRenderer<UsHelmetPasgtItem?>(UsHelmetPasgtModel()) {
    init {
        this.head = GeoBone(null, "", false, 0.0, false, false)
    }

    override fun getRenderType(
        animatable: UsHelmetPasgtItem?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
