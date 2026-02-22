package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity

class Tom6Model : VehicleModel<Tom6Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Tom6Entity>? {
        if (boneName == "melon") {
            return TransformContext { bone, vehicle, _ ->
                bone.isHidden = !vehicle.hasMelon
            }
        }

        return super.collectTransform(boneName)
    }
}
