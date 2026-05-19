package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider

class LavAdRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val rotBarrel = model.getBone("rot_barrel")

        val gunData = vehicle.getGunData(0, 0)
        if (gunData != null) {
            rotBarrel.rotation.rotationZ(-0.5f * (gunData.shootTimer.get() * System.currentTimeMillis() % 36000000) / 75f)
        }

//        "flare" -> TransformContext { bone, vehicle, _ ->
//            val gunData = vehicle.getGunData(0, 0)
//            if (gunData != null) {
//                bone.isHidden = gunData.shootTimer.get() <= 2
//            } else {
//                bone.isHidden = true
//            }
//
//            bone.scaleX = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
//            bone.scaleY = (2 + 0.8 * (Math.random() - 0.5)).toFloat()
//            bone.rotZ = (0.5 * (Math.random() - 0.5)).toFloat()
//        }
    }
}
