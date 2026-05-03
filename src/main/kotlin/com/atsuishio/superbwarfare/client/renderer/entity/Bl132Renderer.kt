package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Bl132Model
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Bl132Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Bl132Entity>(renderManager, Bl132Model())
