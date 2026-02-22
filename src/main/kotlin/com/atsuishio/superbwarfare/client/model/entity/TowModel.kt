package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.CameraType

class TowModel : VehicleModel<TowEntity>() {
    override fun collectTransform(boneName: String): TransformContext<TowEntity>? {
        return when (boneName) {
            "guanmiao" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden =
                    vehicle.getFirstPassenger() === localPlayer && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)
            }

            "missile" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = !vehicle.getEntityData().get(TowEntity.LOADED)
            }

            else -> super.collectTransform(boneName)
        }
    }
}
