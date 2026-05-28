package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.Ju87Entity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Ju87Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : Ju87Entity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val root = model.getBone("root")
        root.visible = !(ClientEventHandler.zoomVehicle && vehicle.firstPassenger == Minecraft.getInstance().player
                && (vehicle.getWeaponIndex(0) == 1
                || vehicle.getWeaponIndex(0) == 2))

        val wingLR = model.getBone("wingLR")
        val wingLR2 = model.getBone("wingLR2")
        val wingLR3 = model.getBone("wingLR3")

        wingLR.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2LRotO,
                vehicle.flap2LRot
            ) * Mth.DEG_TO_RAD
        )

        wingLR2.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2LRotO,
                vehicle.flap2LRot
            ) * Mth.DEG_TO_RAD
        )

        wingLR3.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2LRotO,
                vehicle.flap2LRot
            ) * Mth.DEG_TO_RAD
        )

        val wingRR = model.getBone("wingRR")
        val wingRR2 = model.getBone("wingRR2")
        val wingRR3 = model.getBone("wingRR3")

        wingRR.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2RRotO,
                vehicle.flap2RRot
            ) * Mth.DEG_TO_RAD
        )

        wingRR2.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2RRotO,
                vehicle.flap2RRot
            ) * Mth.DEG_TO_RAD
        )

        wingRR3.rotation.rotateX(
            1.5f * Mth.lerp(
                partialTicks,
                vehicle.flap2RRotO,
                vehicle.flap2RRot
            ) * Mth.DEG_TO_RAD
        )

        val wingLB = model.getBone("wingLB")

        wingLB.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * Mth.DEG_TO_RAD)

        val wingRB = model.getBone("wingRB")

        wingRB.rotation.rotateX(Mth.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * Mth.DEG_TO_RAD)

        val breakerL = model.getBone("breakerL")
        val breakerR = model.getBone("breakerR")

        breakerL.rotation.rotateX(2 * vehicle.planeBreak * Mth.DEG_TO_RAD)
        breakerR.rotation.rotateX(2 * vehicle.planeBreak * Mth.DEG_TO_RAD)

        val tailWing = model.getBone("tailWing")

        tailWing.rotation.rotateY(
            Mth.clamp(
                Mth.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot),
                -20f,
                20f
            ) * Mth.DEG_TO_RAD
        )

        val propeller = model.getBone("propeller")
        val propeller2 = model.getBone("propeller2")
        val propeller3 = model.getBone("propeller3")

        propeller.rotation.rotateZ(-Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))

        val rot = Mth.lerp(partialTicks, vehicle.smallPropellerO, vehicle.smallPropeller)

        propeller2.rotation.rotateZ(-rot)
        propeller3.rotation.rotateZ(rot)
    }
}