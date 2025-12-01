package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Yx100GlowLayer
import com.atsuishio.superbwarfare.client.model.entity.Yx100Model
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Yx100Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Yx100Entity>(renderManager, Yx100Model()) {

    init {
        this.addRenderLayer(Yx100GlowLayer(this))
    }
}
