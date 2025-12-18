package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11HeatLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11Layer
import com.atsuishio.superbwarfare.client.model.entity.Hpj11Model
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Hpj11Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Hpj11Entity>(renderManager, Hpj11Model()) {

    init {
        this.addRenderLayer(Hpj11Layer(this))
        this.addRenderLayer(Hpj11HeatLayer(this))
    }
}
