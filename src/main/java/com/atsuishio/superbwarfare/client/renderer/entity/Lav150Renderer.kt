package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Lav150Layer
import com.atsuishio.superbwarfare.client.model.entity.Lav150Model
import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Lav150Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Lav150Entity>(renderManager, Lav150Model()) {

    init {
        this.addRenderLayer(Lav150Layer(this))
    }
}
