package com.atsuishio.superbwarfare.client.renderer.armor

import com.atsuishio.superbwarfare.client.model.armor.GeHelmetM35Model
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35Item
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoArmorRenderer

class GeHelmetM35ArmorRenderer : GeoArmorRenderer<GeHelmetM35Item?>(GeHelmetM35Model()) {
    init {
        this.head = GeoBone(null, "", false, 0.0, false, false)
    }

    override fun getRenderType(
        animatable: GeHelmetM35Item?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
