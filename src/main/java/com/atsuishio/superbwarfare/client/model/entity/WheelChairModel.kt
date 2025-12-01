package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity

class WheelChairModel : VehicleModel<WheelChairEntity>() {
    override fun collectTransform(boneName: String): TransformContext<WheelChairEntity>? {
        return when (boneName) {
            "w_rb" -> TransformContext { bone, vehicle, _ ->
                bone.rotX = vehicle.rightWheelRot
            }

            "w_lb" -> TransformContext { bone, vehicle, _ ->
                bone.rotX = vehicle.leftWheelRot
            }

            "w_rr" -> TransformContext { bone, vehicle, _ ->
                bone.rotX = 4 * vehicle.rightWheelRot
            }

            "w_lr" -> TransformContext { bone, vehicle, _ ->
                bone.rotX = 4 * vehicle.leftWheelRot
            }

            else -> super.collectTransform(boneName)
        }
    }
}
