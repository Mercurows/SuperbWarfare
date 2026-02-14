package com.atsuishio.superbwarfare.client.renderer.armor

import com.atsuishio.superbwarfare.client.model.armor.RuHelmet6b47Model
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47Item
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoArmorRenderer

class RuHelmet6b47ArmorRenderer : GeoArmorRenderer<RuHelmet6b47Item?>(RuHelmet6b47Model()) {
    init {
        this.head = GeoBone(null, "", false, 0.0, false, false)
    }

    override fun getRenderType(
        animatable: RuHelmet6b47Item?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
