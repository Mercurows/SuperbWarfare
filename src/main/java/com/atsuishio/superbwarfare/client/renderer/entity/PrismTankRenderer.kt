package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.PrismTankLaserLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.PrismTankLightLayer
import com.atsuishio.superbwarfare.client.model.entity.PrismTankModel
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class PrismTankRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<PrismTankEntity>(renderManager, PrismTankModel()) {

    init {
        this.addRenderLayer(PrismTankLaserLayer(this))
        this.addRenderLayer(PrismTankLightLayer(this))
    }
}
