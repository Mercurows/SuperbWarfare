package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth

class A10Model : VehicleModel<A10Entity>() {
    override fun collectTransform(boneName: String): TransformContext<A10Entity>? {
        return when (boneName) {
            "root" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 2
            }

            "wingLR" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap1LRotO,
                    vehicle.flap1LRot
                ) * Mth.DEG_TO_RAD
            }

            "wingRR" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap1RRotO,
                    vehicle.flap1RRot
                ) * Mth.DEG_TO_RAD
            }

            "wingLR2" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap1L2RotO,
                    vehicle.flap1L2Rot
                ) * Mth.DEG_TO_RAD
            }

            "wingRR2" -> TransformContext { bone, vehicle, state ->
                bone.rotX = 1.5f * Mth.lerp(
                    state.partialTick,
                    vehicle.flap1R2RotO,
                    vehicle.flap1R2Rot
                ) * Mth.DEG_TO_RAD
            }

            "wingLB" -> TransformContext { bone, vehicle, state ->
                bone.rotX = Mth.lerp(state.partialTick, vehicle.flap2LRotO, vehicle.flap2LRot) * Mth.DEG_TO_RAD
            }

            "wingRB" -> TransformContext { bone, vehicle, state ->
                bone.rotX = Mth.lerp(state.partialTick, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD
            }

            "weiyiL", "weiyiR" -> TransformContext { bone, vehicle, state ->
                bone.rotY = Mth.clamp(
                    Mth.lerp(state.partialTick, vehicle.flap3RotO, vehicle.flap3Rot),
                    -20f,
                    20f
                ) * Mth.DEG_TO_RAD
            }

            "gear", "gear2", "gear3" -> TransformContext { bone, vehicle, state ->
                bone.rotX = vehicle.gearRot(state.partialTick) * Mth.DEG_TO_RAD
            }

            "qianzhou", "qianzhou2" -> TransformContext { bone, vehicle, state ->
                bone.rotZ = Mth.lerp(state.partialTick, vehicle.propellerRotO, vehicle.propellerRot)
            }

            "bomb1" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 3)
            }

            "bomb2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 2)
            }

            "bomb3" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideBomb(vehicle, 1)
            }

            "missile1" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 4)
            }

            "missile2" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 3)
            }

            "missile3" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 2)
            }

            "missile4" -> TransformContext { bone, vehicle, _ ->
                bone.isHidden = shouldHideMissile(vehicle, 1)
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

