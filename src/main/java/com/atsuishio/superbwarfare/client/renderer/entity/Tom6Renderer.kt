package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Tom6Model
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Tom6Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Tom6Entity>(renderManager, Tom6Model()) {

    init {
        this.shadowRadius = 0.5f
    }
}
