package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.model.item.VehicleAssemblingTableItemModel
import com.atsuishio.superbwarfare.item.blockitem.VehicleAssemblingTableBlockItem
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoItemRenderer

class VehicleAssemblingTableBlockItemRenderer :
    GeoItemRenderer<VehicleAssemblingTableBlockItem>(VehicleAssemblingTableItemModel()) {
    override fun getRenderType(
        animatable: VehicleAssemblingTableBlockItem,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }
}
