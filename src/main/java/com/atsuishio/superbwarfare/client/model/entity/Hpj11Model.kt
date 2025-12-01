package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.MC
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.CameraType
import net.minecraft.world.entity.player.Player

class Hpj11Model : VehicleModel<Hpj11Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Hpj11Entity>? {
        return when (boneName) {
            "radar2" -> TransformContext { bone, vehicle, _ ->
                val player: Player? = MC.player
                bone.setHidden(vehicle.getNthEntity(vehicle.turretControllerIndex) === player && (MC.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle))
            }

            "rdr", "rdr2" -> TransformContext { bone, _, _ ->
                bone.setRotX(animationProcessor.getBone("barrel").rotX)
            }

            "paoguanroll" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0, 0)
                if (gunData != null) {
                    bone.setRotZ(bone.rotZ + gunData.shootTimer.get())
                }
            }

            "flare" -> TransformContext { bone, vehicle, _ ->
                val gunData = vehicle.getGunData(0)
                if (gunData != null) {
                    bone.setHidden(gunData.shootTimer.get() <= 2)
                } else {
                    bone.setHidden(true)
                }

                bone.setScaleX((2 + 0.8 * (Math.random() - 0.5)).toFloat())
                bone.setScaleY((2 + 0.8 * (Math.random() - 0.5)).toFloat())
                bone.setRotZ((0.5 * (Math.random() - 0.5)).toFloat())
            }

            else -> super.collectTransform(boneName)
        }
    }
}
