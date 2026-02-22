package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity
import java.util.regex.Pattern

class AnnihilatorModel : VehicleModel<AnnihilatorEntity>() {
    companion object {
        private val LED_PATTERN: Pattern = Pattern.compile("led(?<type>green|red)(?<id>\\d+)")
    }

    override fun collectTransform(boneName: String): TransformContext<AnnihilatorEntity>? {
        return when (boneName) {
            "laser1" -> TransformContext { bone, vehicle, _ ->
                bone.scaleZ = vehicle.getEntityData().get(AnnihilatorEntity.LASER_LEFT_LENGTH)
            }

            "laser2" -> TransformContext { bone, vehicle, _ ->
                bone.scaleZ = vehicle.getEntityData().get(AnnihilatorEntity.LASER_MIDDLE_LENGTH)
            }

            "laser3" -> TransformContext { bone, vehicle, _ ->
                bone.scaleZ = vehicle.getEntityData().get(AnnihilatorEntity.LASER_RIGHT_LENGTH)
            }

            else -> {
                val matcher = LED_PATTERN.matcher(boneName)
                if (matcher.matches()) {
                    val isGreen = matcher.group("type") == "green"
                    val id = matcher.group("id").toInt()

                    return TransformContext { bone, vehicle, _ ->
                        val charge = vehicle.chargeProgress
                        val cantShoot = charge > 1

                        val hideGreen = 5 * charge < id || cantShoot
                        bone.isHidden = isGreen == hideGreen
                    }
                }

                super.collectTransform(boneName)
            }
        }
    }
}
