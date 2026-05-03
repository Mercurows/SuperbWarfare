package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.WheelChairModel
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class WheelChairRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<WheelChairEntity>(renderManager, WheelChairModel()) {

    init {
        this.shadowRadius = 0.5f
    }
}
