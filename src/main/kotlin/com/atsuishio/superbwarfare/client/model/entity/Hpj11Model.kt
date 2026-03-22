package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.CameraType

class Hpj11Model : VehicleModel<Hpj11Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Hpj11Entity>? {
        return when (boneName) {
            "radar2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden =
                    vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)
            }

            "rdr", "rdr2" -> TransformContext { bone, _, _ ->
                bone.rotX = animationProcessor.getBone("barrel").rotX
            }

            "paoguanroll" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0, 0)
                if (gunData != null) {
                    bone.rotZ += gunData.shootTimer.get()
                }
            }

            "flare" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0)
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
