package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatWaterMaskLayer
import com.atsuishio.superbwarfare.client.model.entity.SpeedboatModel
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class SpeedboatRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<SpeedboatEntity>(renderManager, SpeedboatModel()) {

    init {
        this.addRenderLayer(SpeedBoatLayer(this))
        this.addRenderLayer(SpeedBoatWaterMaskLayer(this))
    }
}
