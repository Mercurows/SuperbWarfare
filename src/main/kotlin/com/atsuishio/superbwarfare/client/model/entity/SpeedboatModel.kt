package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.util.Mth

class SpeedboatModel : VehicleModel<SpeedboatEntity>() {
    override fun collectTransform(boneName: String): TransformContext<SpeedboatEntity>? {
        if (boneName == "propeller") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }
        }

        if (boneName == "turret") {
            return TransformContext { bone, vehicle, _ ->
                bone.rotY = turretYRot * Mth.DEG_TO_RAD
                bone.isHidden =
                    vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer && ClientEventHandler.zoomVehicle
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
