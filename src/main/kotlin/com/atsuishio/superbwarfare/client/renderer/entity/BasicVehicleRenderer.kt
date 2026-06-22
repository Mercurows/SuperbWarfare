package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

open class BasicVehicleRenderer(manager: EntityRendererProvider.Context) : SbmVehicleRenderer<VehicleEntity>(manager)