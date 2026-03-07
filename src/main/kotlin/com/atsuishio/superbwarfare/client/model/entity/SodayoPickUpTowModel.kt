package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpTowEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.CameraType
import net.minecraft.util.Mth

class SodayoPickUpTowModel : VehicleModel<SodayoPickUpTowEntity>() {

    override fun collectTransform(boneName: String): TransformContext<SodayoPickUpTowEntity>? {
        if (boneName == "control") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = 8 * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        if (boneName == "head") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ += (0.2f * Mth.lerp(
                    state.partialTick,
                    vehicle.rudderRotO,
                    vehicle.rudderRot
                ) * vehicle.deltaMovement.horizontalDistance()).toFloat()
                bone.rotZ *= 0.8f
                bone.rotX += -2f * vehicle.getAcceleration().toFloat()
                bone.rotX *= 0.8f
            }
        }

        if (boneName == "guanmiao") {
            return TransformContext { bone, vehicle, _ ->
                bone.isHidden =
                    vehicle.turretControllerIndex == vehicle.getSeatIndex(localPlayer) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)
            }
        }

        return super.collectTransform(boneName)
    }
}
