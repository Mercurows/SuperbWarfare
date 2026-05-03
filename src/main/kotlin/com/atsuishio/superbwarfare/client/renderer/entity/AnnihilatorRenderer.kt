package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.*
import com.atsuishio.superbwarfare.client.model.entity.AnnihilatorModel
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class AnnihilatorRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<AnnihilatorEntity>(renderManager, AnnihilatorModel()) {

    init {
        this.addRenderLayer(AnnihilatorLayer(this))
        this.addRenderLayer(AnnihilatorGlowLayer(this))
        this.addRenderLayer(AnnihilatorPowerLayer(this))
        this.addRenderLayer(AnnihilatorPowerLightLayer(this))
        this.addRenderLayer(AnnihilatorLedLayer(this))
        this.addRenderLayer(AnnihilatorLedLightLayer(this))
    }
}
