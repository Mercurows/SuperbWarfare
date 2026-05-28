package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class A10Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : A10Entity, T : BasicGeoVehicleEntity {

    override fun renderCustomPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)
    }

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val root = model.getBone("root")
        root.visible = !(hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 2)

        val wingLR = model.getBone("wingLR")

        wingLR.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap1LRotO,
                vehicle.flap1LRot
            ) * Mth.DEG_TO_RAD
        )

        val wingRR = model.getBone("wingRR")

        wingRR.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap1RRotO,
                vehicle.flap1RRot
            ) * Mth.DEG_TO_RAD
        )

        val wingLR2 = model.getBone("wingLR2")

        wingLR2.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap1L2RotO,
                vehicle.flap1L2Rot
            ) * Mth.DEG_TO_RAD
        )

        val wingRR2 = model.getBone("wingRR2")

        wingRR2.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap1R2RotO,
                vehicle.flap1R2Rot
            ) * Mth.DEG_TO_RAD
        )

        val wingLB = model.getBone("wingLB")

        wingLB.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * Mth.DEG_TO_RAD)

        val wingRB = model.getBone("wingRB")

        wingRB.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD)

        val weiyiL = model.getBone("weiyiL")
        val weiyiR = model.getBone("weiyiR")

        weiyiL.rotation.rotateY(
            Mth.clamp(
                Mth.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot),
                -20f,
                20f
            ) * Mth.DEG_TO_RAD
        )

        weiyiR.rotation.rotateY(
            Mth.clamp(
                Mth.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot),
                -20f,
                20f
            ) * Mth.DEG_TO_RAD
        )

        val gear = model.getBone("gear")
        val gear2 = model.getBone("gear2")
        val gear3 = model.getBone("gear3")

        gear.rotation.rotationX(vehicle.gearRot(partialTicks) * Mth.DEG_TO_RAD)
        gear2.rotation.rotationX(vehicle.gearRot(partialTicks) * Mth.DEG_TO_RAD)
        gear3.rotation.rotationX(vehicle.gearRot(partialTicks) * Mth.DEG_TO_RAD)

        val qianzhou = model.getBone("qianzhou")
        val qianzhou2 = model.getBone("qianzhou2")

        qianzhou.rotation.rotateZ(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        qianzhou2.rotation.rotateZ(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    }
}
