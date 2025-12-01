package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import java.util.regex.Pattern

class WaveforceTowerModel : VehicleModel<WaveforceTowerEntity>() {
    var energy0: Float = 0f
    private val LIGHT_PATTERN: Pattern = Pattern.compile("^light_(?<type>on|off)(?<id>\\d+)")

    override fun collectTransform(boneName: String): TransformContext<WaveforceTowerEntity>? {
        when (boneName) {
            "glow" -> {
                return TransformContext { bone, vehicle, state ->
                    val scale = Mth.lerp(
                        state.partialTick,
                        vehicle.getEntityData().get(VehicleEntity.LASER_SCALE_O),
                        vehicle.getEntityData().get(VehicleEntity.LASER_SCALE)
                    ).coerceAtMost(1.2f)

                    bone.scaleX = scale
                    bone.scaleY = scale
                    bone.scaleZ = scale
                }
            }

            "glow2" -> {
                return TransformContext { bone, vehicle, state ->
                    bone.posZ = -16f * vehicle.getEntityData().get(VehicleEntity.LASER_LENGTH)
                    val scale = Mth.lerp(
                        state.partialTick,
                        vehicle.getEntityData().get(VehicleEntity.LASER_SCALE_O),
                        vehicle.getEntityData().get(VehicleEntity.LASER_SCALE)
                    ).coerceAtMost(1.2f)

                    bone.scaleX = scale
                    bone.scaleY = scale
                    bone.scaleZ = scale
                }
            }

            "charge" -> {
                return TransformContext { bone, vehicle, state ->
                    val energy = vehicle.getEntityData().get(VehicleEntity.CHARGE_PROGRESS)
                    val energyRate0 = energy0
                    bone.scaleZ = Mth.lerp(state.partialTick, energyRate0, energy)
                    energy0 = energy
                }
            }
        }

        val matcher = LIGHT_PATTERN.matcher(boneName)
        if (matcher.matches()) {
            val isOn = matcher.group("type") == "on"
            val index = matcher.group("id").toInt()

            return TransformContext { bone, vehicle, _ ->
                val energy = vehicle.getEntityData().get(VehicleEntity.CHARGE_PROGRESS)
                val shouldTurnOn = energy >= index / 7f
                bone.isHidden = shouldTurnOn != isOn
            }
        }

        return super.collectTransform(boneName)
    }
}
