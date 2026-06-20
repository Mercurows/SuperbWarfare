package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.Ac130hEntity
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Ac130hRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : Ac130hEntity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val wingFL = model.getBone("wingFL")
        val wingFR = model.getBone("wingFR")
        val xRot = -1.5f * Mth.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD

        wingFL.rotation.rotateX(xRot)
        wingFR.rotation.rotateX(xRot)

        val tailWingHL = model.getBone("tailWingHL")

        tailWingHL.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * Mth.DEG_TO_RAD)

        val tailWingHR = model.getBone("tailWingHR")

        tailWingHR.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD)

        val tailWingV = model.getBone("tailWingV")

        tailWingV.rotation.rotateY(
            Mth.clamp(
                Mth.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot),
                -20f,
                20f
            ) * Mth.DEG_TO_RAD
        )

        val propeller = model.getBone("prop1")
        val propeller2 = model.getBone("prop2")
        val propeller3 = model.getBone("prop3")
        val propeller4 = model.getBone("prop4")
        val rot = Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)

        propeller.rotation.rotateZ(rot)
        propeller2.rotation.rotateZ(rot)
        propeller3.rotation.rotateZ(rot)
        propeller4.rotation.rotateZ(rot)

    }
}