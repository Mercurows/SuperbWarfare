package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Ju87Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth

class Ju87Model : VehicleModel<Ju87Entity>() {
    override fun collectTransform(boneName: String): TransformContext<Ju87Entity>? {
        return when (boneName) {
            "root" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden =
                    ClientEventHandler.zoomVehicle && vehicle.firstPassenger == Minecraft.getInstance().player && (vehicle.getWeaponIndex(
                        0
                    ) == 1 || vehicle.getWeaponIndex(0) == 2)
            }

            "wingLR", "wingLR2", "wingLR3" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap2LRotO,
                    vehicle.flap2LRot
                ) * Mth.DEG_TO_RAD
            }

            "wingRR", "wingRR2", "wingRR3" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap2RRotO,
                    vehicle.flap2RRot
                ) * Mth.DEG_TO_RAD
            }

            "breakerL", "breakerR" -> TransformContext { bone, vehicle, _ ->
                bone.rotX = 2 * vehicle.planeBreak * Mth.DEG_TO_RAD
            }

            "wingLB" -> TransformContext { bone, vehicle, state ->
                bone.rotX = Mth.lerp(state.partialTick, vehicle.flap2LRotO, vehicle.flap2LRot) * Mth.DEG_TO_RAD
            }

            "wingRB" -> TransformContext { bone, vehicle, state ->
                bone.rotX = Mth.lerp(state.partialTick, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD
            }

            "tailWing" -> TransformContext { bone, vehicle, state ->
                bone.rotY = Mth.clamp(
                    Mth.lerp(state.partialTick, vehicle.flap3RotO, vehicle.flap3Rot),
                    -20f,
                    20f
                ) * Mth.DEG_TO_RAD
            }

            "propeller" -> TransformContext { bone, vehicle, state ->
                bone.rotZ = -Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }

            "propeller2" -> TransformContext { bone, vehicle, _ ->
                bone.rotZ -= vehicle.deltaMovement.dot(vehicle.lookAngle).toFloat() * 0.5f
            }

            "propeller3" -> TransformContext { bone, vehicle, _ ->
                bone.rotZ += vehicle.deltaMovement.dot(vehicle.lookAngle).toFloat() * 0.5f
            }

            "bomb1" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 4)
            }

            "bomb2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 3)
            }

            "bomb3" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 2)
            }

            "bomb4" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 1)
            }

            "bomb5" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBigBomb(vehicle, 1)
            }

            else -> super.collectTransform(boneName)
        }
    }

    private fun shouldHideBomb(vehicle: VehicleEntity, ammo: Int): Boolean {
        val gunData = vehicle.getGunData("BombSmall") ?: return false
        return gunData.ammo.get() < ammo
    }

    private fun shouldHideBigBomb(vehicle: VehicleEntity, ammo: Int): Boolean {
        val gunData = vehicle.getGunData("Bomb") ?: return false
        return gunData.ammo.get() < ammo
    }
}

