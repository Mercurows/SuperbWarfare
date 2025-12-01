package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity

class DroneModel : VehicleModel<DroneEntity>() {
    override fun collectTransform(boneName: String): TransformContext<DroneEntity>? {
        return when (boneName) {
            "wingFL", "wingFR", "wingBL", "wingBR" -> TransformContext { bone, _, _ ->
                bone.rotY = (System.currentTimeMillis() % 36000000) / 12f
            }

            else -> super.collectTransform(boneName)
        }
    }
}
