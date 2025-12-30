package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModel.TransformContext
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpEntity
import net.minecraft.util.Mth

class SodayoPickUpModel : VehicleModel<SodayoPickUpEntity>() {
    
    override fun collectTransform(boneName: String): TransformContext<SodayoPickUpEntity>? {
        if (boneName == "control") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = 8 * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        if (boneName == "head") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ += (0.2f * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot) * vehicle.deltaMovement.dot(vehicle.getViewVector(1f))).toFloat()
                bone.rotZ *= 0.8f
                bone.rotX += -0.1f * vehicle.getAcceleration().toFloat()
                bone.rotX *= 0.8f
            }
        }

        return super.collectTransform(boneName)
    }
}
