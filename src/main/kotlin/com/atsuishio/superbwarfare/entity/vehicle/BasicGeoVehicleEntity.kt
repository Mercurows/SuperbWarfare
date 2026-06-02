package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance

interface BasicGeoVehicleEntity {
    fun getAnimationInstance(): VehicleAnimationInstance<*>? = null
}