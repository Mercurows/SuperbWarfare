package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.M1A2Layer
import com.atsuishio.superbwarfare.client.model.entity.M1A2Model
import com.atsuishio.superbwarfare.entity.vehicle.M1A2Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class M1A2Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<M1A2Entity>(renderManager, M1A2Model()) {
    init {
        this.addRenderLayer(M1A2Layer(this))
    }
}
