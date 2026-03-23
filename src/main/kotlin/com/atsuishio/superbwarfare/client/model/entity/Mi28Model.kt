package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Mi28Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth

class Mi28Model : VehicleModel<Mi28Entity>() {
    override fun hideForTurretControllerWhileZooming() = true

    override fun collectTransform(boneName: String): TransformContext<Mi28Entity>? {
        return when (boneName) {
            "propeller" -> TransformContext { bone, vehicle, state ->
                bone.rotY = -Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }

            "tailPropeller" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 6 * Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }

            "missile1" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 2)
            }

            "missile2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 1)
            }

            else -> super.collectTransform(boneName)
        }
    }

    fun shouldHideMissile(vehicle: VehicleEntity, ammo: Int): Boolean {
        val gunData = vehicle.getGunData("SeekMissile") ?: return false
        return gunData.ammo.get() < ammo
    }
}
