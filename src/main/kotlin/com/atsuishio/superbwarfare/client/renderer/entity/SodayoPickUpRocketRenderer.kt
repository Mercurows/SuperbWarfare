package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.SodayoPickUpRocketModel
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class SodayoPickUpRocketRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<SodayoPickUpRocketEntity>(renderManager, SodayoPickUpRocketModel())


