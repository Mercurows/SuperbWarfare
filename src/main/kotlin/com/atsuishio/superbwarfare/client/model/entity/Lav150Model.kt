package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity

class Lav150Model : VehicleModel<Lav150Entity>() {
    override fun hideForTurretControllerWhileZooming() = true
}
