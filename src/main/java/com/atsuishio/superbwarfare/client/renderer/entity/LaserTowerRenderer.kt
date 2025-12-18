package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerLaserLayer
import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerPowerLayer
import com.atsuishio.superbwarfare.client.model.entity.LaserTowerModel
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class LaserTowerRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<LaserTowerEntity>(renderManager, LaserTowerModel()) {

    init {
        this.addRenderLayer(LaserTowerPowerLayer(this))
        this.addRenderLayer(LaserTowerLaserLayer(this))
    }
}
