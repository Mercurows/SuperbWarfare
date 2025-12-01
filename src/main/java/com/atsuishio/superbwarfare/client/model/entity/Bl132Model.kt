package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity

class Bl132Model : VehicleModel<Bl132Entity>() {
    @Deprecated("Deprecated in Java")
    override fun getTextureResource(vehicle: Bl132Entity) = if (vehicle.getUUID().leastSignificantBits % 50 == 0L) {
        loc("textures/entity/bl_132_black.png")
    } else {
        loc("textures/entity/bl_132.png")
    }

    override fun hideForTurretControllerWhileZooming() = true
}
