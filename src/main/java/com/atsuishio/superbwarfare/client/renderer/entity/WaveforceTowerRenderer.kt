package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerGlowLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerLaserLayer
import com.atsuishio.superbwarfare.client.model.entity.WaveforceTowerModel
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class WaveforceTowerRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<WaveforceTowerEntity>(renderManager, WaveforceTowerModel()) {

    init {
        this.addRenderLayer(WaveforceTowerGlowLayer(this))
        this.addRenderLayer(WaveforceTowerLaserLayer(this))
    }
}
