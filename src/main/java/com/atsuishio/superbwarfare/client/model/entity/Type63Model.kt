package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import java.util.regex.Pattern

class Type63Model : VehicleModel<Type63Entity>() {
    private val SHELL_PATTERN: Pattern = Pattern.compile("^shell(?<id>\\d+)$")

    override fun collectTransform(boneName: String): TransformContext<Type63Entity>? {
        if (boneName == "shoulunx") {
            return TransformContext { bone, _, _ ->
                bone.rotX = -turretXRot * 3
            }
        }

        if (boneName == "shouluny") {
            return TransformContext { bone, _, _ ->
                bone.rotZ = -turretYRot * 6
            }
        }

        val matcher = SHELL_PATTERN.matcher(boneName)
        if (matcher.matches()) {
            return TransformContext { bone, vehicle, _ ->
                val items = vehicle.getEntityData().get(Type63Entity.LOADED_AMMO)
                val i = matcher.group("id").toInt()
                bone.isHidden = items[i] == -1
            }
        }

        return super.collectTransform(boneName)
    }
}
