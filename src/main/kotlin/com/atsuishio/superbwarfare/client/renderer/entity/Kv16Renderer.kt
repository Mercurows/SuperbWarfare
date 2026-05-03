package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Kv16Model
import com.atsuishio.superbwarfare.entity.vehicle.Kv16Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Kv16Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Kv16Entity>(renderManager, Kv16Model()) {

    init {
        this.shadowRadius = 0.5f
    }
}
