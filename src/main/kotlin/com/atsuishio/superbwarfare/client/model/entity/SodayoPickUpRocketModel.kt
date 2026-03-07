package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import net.minecraft.util.Mth
import java.util.regex.Pattern

class SodayoPickUpRocketModel : VehicleModel<SodayoPickUpRocketEntity>() {
    private val SHELL_PATTERN: Pattern = Pattern.compile("^shell(?<id>\\d+)$")
    override fun collectTransform(boneName: String): TransformContext<SodayoPickUpRocketEntity>? {
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

        val matcher = SHELL_PATTERN.matcher(boneName)
        if (matcher.matches()) {
            return TransformContext { bone, vehicle, _ ->
                val items = vehicle.getEntityData().get(SodayoPickUpRocketEntity.LOADED_AMMO)
                val i = matcher.group("id").toInt()
                bone.isHidden = items[i] == -1
            }
        }

        return super.collectTransform(boneName)
    }
}
