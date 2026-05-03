package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Lav25Layer
import com.atsuishio.superbwarfare.client.model.entity.Lav25Model
import com.atsuishio.superbwarfare.entity.vehicle.Lav25Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Lav25Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Lav25Entity>(renderManager, Lav25Model()) {

    init {
        this.addRenderLayer(Lav25Layer(this))
    }
}
