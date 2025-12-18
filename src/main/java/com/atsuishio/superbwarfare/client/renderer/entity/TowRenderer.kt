package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.TowModel
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class TowRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<TowEntity>(renderManager, TowModel())
