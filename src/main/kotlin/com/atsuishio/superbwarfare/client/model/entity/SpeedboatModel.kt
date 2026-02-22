package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity
import net.minecraft.util.Mth

class SpeedboatModel : VehicleModel<SpeedboatEntity>() {
    override fun collectTransform(boneName: String): TransformContext<SpeedboatEntity>? {
        if (boneName == "propeller") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        if (boneName == "rudder") {
            return TransformContext { bone, vehicle, state ->
                bone.rotY = Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        return super.collectTransform(boneName)
    }
}
