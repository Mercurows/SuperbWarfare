package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity
import net.minecraft.util.Mth

class Ah6Model : VehicleModel<Ah6Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Ah6Entity>? {
        if (boneName == "propeller") {
            return TransformContext { bone, vehicle, state ->
                bone.rotY = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        if (boneName == "tailPropeller") {
            return TransformContext { bone, vehicle, state ->
                bone.rotX = -6 * Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        return super.collectTransform(boneName)
    }
}
