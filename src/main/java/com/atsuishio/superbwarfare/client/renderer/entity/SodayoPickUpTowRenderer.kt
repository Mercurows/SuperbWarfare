package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.SodayoPickUpTowModel
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpTowEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class SodayoPickUpTowRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<SodayoPickUpTowEntity>(renderManager, SodayoPickUpTowModel())


