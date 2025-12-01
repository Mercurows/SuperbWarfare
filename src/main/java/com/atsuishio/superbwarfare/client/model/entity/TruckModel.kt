package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

class TruckModel : VehicleModel<TruckEntity>() {
    @Deprecated("Deprecated in Java")
    override fun getTextureResource(vehicle: TruckEntity): ResourceLocation {
        return if (vehicle.getEntityData().get(TruckEntity.GREEN)) {
            loc("textures/entity/truck_green.png")
        } else {
            loc("textures/entity/truck_red.png")
        }
    }

    override fun collectTransform(boneName: String): TransformContext<TruckEntity>? {
        if (boneName == "control") {
            return TransformContext { bone, vehicle, state ->
                bone.rotY = 12 * Mth.lerp(state.partialTick, vehicle.rudderRotO, vehicle.rudderRot)
            }
        }

        return super.collectTransform(boneName)
    }
}
