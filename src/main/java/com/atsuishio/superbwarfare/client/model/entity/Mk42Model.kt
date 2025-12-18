package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity

class Mk42Model : VehicleModel<Mk42Entity>() {
    override fun hideForTurretControllerWhileZooming() = true
}
