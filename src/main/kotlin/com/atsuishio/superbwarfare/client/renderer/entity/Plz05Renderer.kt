package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Plz05Model
import com.atsuishio.superbwarfare.entity.vehicle.Plz05Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Plz05Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Plz05Entity>(renderManager, Plz05Model())
