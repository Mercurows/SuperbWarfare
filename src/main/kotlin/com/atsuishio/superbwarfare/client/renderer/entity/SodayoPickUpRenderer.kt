package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.SodayoPickUpModel
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class SodayoPickUpRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<SodayoPickUpEntity>(renderManager, SodayoPickUpModel())
