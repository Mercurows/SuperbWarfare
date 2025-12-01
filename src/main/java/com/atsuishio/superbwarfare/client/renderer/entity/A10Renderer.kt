package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.A10Model
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class A10Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<A10Entity>(renderManager, A10Model()) {

    init {
        this.shadowRadius = 0.5f
    }
}
