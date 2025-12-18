package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Bmp2Layer
import com.atsuishio.superbwarfare.client.model.entity.Bmp2Model
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Bmp2Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Bmp2Entity>(renderManager, Bmp2Model()) {

    init {
        this.addRenderLayer(Bmp2Layer(this))
    }
}
