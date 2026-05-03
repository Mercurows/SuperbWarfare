package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.TinySpeedBoatColorLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.TinySpeedBoatWaterMaskLayer
import com.atsuishio.superbwarfare.client.model.entity.TinySpeedboatModel
import com.atsuishio.superbwarfare.entity.vehicle.TinySpeedboatEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class TinySpeedboatRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<TinySpeedboatEntity>(renderManager, TinySpeedboatModel()) {
    init {
        this.addRenderLayer(TinySpeedBoatWaterMaskLayer(this))
        this.addRenderLayer(TinySpeedBoatColorLayer(this))
    }
}
