package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity

class Mle1934Model : VehicleModel<Mle1934Entity>() {
    override fun hideForTurretControllerWhileZooming() = true
}
