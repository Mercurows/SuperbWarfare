package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.LavAdLayer
import com.atsuishio.superbwarfare.client.model.entity.LavAdModel
import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class LavAdRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<LavAdEntity>(renderManager, LavAdModel()) {

    init {
        this.addRenderLayer(LavAdLayer(this))
    }
}
