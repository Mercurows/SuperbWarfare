package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.TinySpeedboatEntity
import net.minecraft.util.Mth

class TinySpeedboatModel : VehicleModel<TinySpeedboatEntity>() {
    override fun collectTransform(boneName: String): TransformContext<TinySpeedboatEntity>? {
        if (boneName == "rudder") {
            return TransformContext { bone, vehicle, state ->
                bone.rotY = Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        if (boneName == "control") {
            return TransformContext { bone, vehicle, state ->
                bone.rotZ = -3 * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        return super.collectTransform(boneName)
    }
}
