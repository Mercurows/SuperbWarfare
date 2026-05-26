package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import net.minecraft.resources.ResourceLocation

interface BasicGeoVehicleEntity {
    fun getAnimation(): ResourceLocation? = null

    fun getAnimationInstance(): VehicleAnimationInstance<*>? = null
}