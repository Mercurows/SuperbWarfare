package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity

class LavAdModel : VehicleModel<LavAdEntity>() {
    override fun collectTransform(boneName: String): TransformContext<LavAdEntity>? {
        return when (boneName) {
            "rot_barrel" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0, 0)
                if (gunData != null) {
                    bone.rotZ += 0.3f * gunData.shootTimer.get()
                }
            }

            "flare" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0, 0)
                if (gunData != null) {
                    bone.isHidden = gunData.shootTimer.get() <= 2
                } else {
                    bone.isHidden = true
                }

                bone.scaleX = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
                bone.scaleY = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
                bone.rotZ = (0.5 * (Math.random() - 0.5)).toFloat()
            }

            else -> super.collectTransform(boneName)
        }
    }

    override fun hideForTurretControllerWhileZooming() = true
}
