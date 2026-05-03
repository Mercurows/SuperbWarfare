package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.TruckModel
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class TruckRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<TruckEntity>(renderManager, TruckModel())
