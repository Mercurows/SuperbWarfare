package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.SodayoPickUpHmgLayer
import com.atsuishio.superbwarfare.client.model.entity.SodayoPickUpHmgModel
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpHmgEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class SodayoPickUpHmgRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<SodayoPickUpHmgEntity>(renderManager, SodayoPickUpHmgModel()) {
    init {
        this.addRenderLayer(SodayoPickUpHmgLayer(this))
    }
}


