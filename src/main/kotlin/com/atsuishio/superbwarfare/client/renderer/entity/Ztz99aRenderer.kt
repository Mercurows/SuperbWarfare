package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Ztz99aLayer
import com.atsuishio.superbwarfare.client.model.entity.Ztz99aModel
import com.atsuishio.superbwarfare.entity.vehicle.Ztz99aEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Ztz99aRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Ztz99aEntity>(renderManager, Ztz99aModel()) {
    init {
        this.addRenderLayer(Ztz99aLayer(this))
    }
}
