package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Lav25Entity

class Lav25Model : VehicleModel<Lav25Entity>() {
    override fun hideForTurretControllerWhileZooming() = true
}
