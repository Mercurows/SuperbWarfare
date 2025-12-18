package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Mle1934Model
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Mle1934Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Mle1934Entity>(renderManager, Mle1934Model())
