package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModel.TransformContext
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity
import net.minecraft.util.Mth

class SpeedboatModel : VehicleModel<SpeedboatEntity>() {
    override fun hideForTurretControllerWhileZooming() = true
    override fun collectTransform(boneName: String): TransformContext<SpeedboatEntity>? {
        if (boneName == "propeller") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        if (boneName == "propeller2") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = -Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        if (boneName == "control") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = -4 * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
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
