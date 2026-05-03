package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.BradleyLayer
import com.atsuishio.superbwarfare.client.model.entity.BradleyModel
import com.atsuishio.superbwarfare.entity.vehicle.BradleyEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
class BradleyRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<BradleyEntity>(renderManager, BradleyModel()) {
    init {
        this.addRenderLayer(BradleyLayer(this))
    }
}
