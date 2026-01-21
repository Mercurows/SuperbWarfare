package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.Ju87Model
import com.atsuishio.superbwarfare.entity.vehicle.Ju87Entity
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Ju87Renderer(renderManager: EntityRendererProvider.Context) : VehicleRenderer<Ju87Entity>(renderManager, Ju87Model()) 
