package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.T90aModel
import com.atsuishio.superbwarfare.entity.vehicle.T90aEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class T90aRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<T90aEntity>(renderManager, T90aModel()) {
}
