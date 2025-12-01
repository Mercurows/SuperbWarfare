package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Mi28Model
import com.atsuishio.superbwarfare.entity.vehicle.Mi28Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Mi28Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Mi28Entity>(renderManager, Mi28Model()) {

    init {
        this.shadowRadius = 1f
    }
}
