package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Mk42Model
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Mk42Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Mk42Entity>(renderManager, Mk42Model())
