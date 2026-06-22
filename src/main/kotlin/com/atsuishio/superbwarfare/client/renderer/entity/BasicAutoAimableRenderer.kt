package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

open class BasicAutoAimableRenderer(manager: EntityRendererProvider.Context) : SbmVehicleRenderer<AutoAimableEntity>(manager)