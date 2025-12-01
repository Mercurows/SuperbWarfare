package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.MC
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.CameraType

class TowModel : VehicleModel<TowEntity>() {
    override fun collectTransform(boneName: String): TransformContext<TowEntity>? {
        return when (boneName) {
            "guanmiao" -> TransformContext { bone, vehicle, _ ->
                val player = MC.player
                bone.setHidden(vehicle.getFirstPassenger() === player && (MC.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle))
            }

            "missile" -> TransformContext { bone, vehicle, _ ->
                bone.setHidden(!vehicle.getEntityData().get(TowEntity.LOADED))
            }

            else -> super.collectTransform(boneName)
        }
    }
}
