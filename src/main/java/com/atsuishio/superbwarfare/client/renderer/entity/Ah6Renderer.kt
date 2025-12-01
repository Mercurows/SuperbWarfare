package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Ah6Model
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Ah6Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Ah6Entity>(renderManager, Ah6Model()) {

    init {
        this.shadowRadius = 0.5f
    }
}
