package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Type63Model
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Type63Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Type63Entity>(renderManager, Type63Model()) {

    init {
        this.shadowRadius = 0.8f
    }
}
