package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModel.TransformContext
import com.atsuishio.superbwarfare.entity.vehicle.Kv16Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth

class Kv16Model : VehicleModel<Kv16Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Kv16Entity>? {
        return when (boneName) {
            "root" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 1
            }

            "propeller" -> TransformContext { bone, vehicle, state ->
                bone.rotZ = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }

            "shell" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 6)
            }

            "shell2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 5)
            }

            "shell3" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 4)
            }

            "shell4" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 3)
            }

            "shell5" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 2)
            }

            "shell6" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 1)
            }

            else -> null
        }
    }

    fun shouldHideBomb(vehicle: VehicleEntity, ammo: Int): Boolean {
        val gunData = vehicle.getGunData("Bomb") ?: return false
        return gunData.ammo.get() < ammo
    }

    fun shouldHideMissile(vehicle: VehicleEntity, ammo: Int): Boolean {
        val gunData = vehicle.getGunData("Missile") ?: return false
        return gunData.ammo.get() < ammo
    }
}

