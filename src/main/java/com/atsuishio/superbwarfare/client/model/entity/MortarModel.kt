package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.constant.DataTickets

class MortarModel : VehicleModel<MortarEntity>() {
    override fun collectTransform(boneName: String): TransformContext<MortarEntity>? {
        if (boneName == "paoguan") {
            return TransformContext { bone, _, state ->
                val jiaojia = animationProcessor.getBone("jiaojia")
                val entityData = state.getData(DataTickets.ENTITY_MODEL_DATA)
                if (entityData != null) {
                    bone.rotX = (entityData.headPitch()) * Mth.DEG_TO_RAD
                    jiaojia.rotX =
                        -2 * ((entityData.headPitch() - (10 - entityData.headPitch() * 0.1f)) * Mth.DEG_TO_RAD)
                }
            }
        }

        if (boneName == "monitor") {
            return TransformContext { bone, vehicle, _ ->
                bone.isHidden = !vehicle.getEntityData().get(MortarEntity.INTELLIGENT)
            }
        }

        return super.collectTransform(boneName)
    }
}
