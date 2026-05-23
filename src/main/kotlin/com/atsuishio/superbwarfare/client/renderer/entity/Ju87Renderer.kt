package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.Ju87Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
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

        val rotL = Axis.XP.rotation(1.5f * leftWheelRot)

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

        // TODO 修复小螺旋桨运动

        propeller2.rotation.rotateZ(-0.5f * (vehicle.deltaMovement.dot(vehicle.lookAngle).toFloat() * System.currentTimeMillis() % 36000000) / 75f)
        propeller3.rotation.rotateZ(0.5f * (vehicle.deltaMovement.dot(vehicle.lookAngle).toFloat() * System.currentTimeMillis() % 36000000) / 75f)

        val bomb1 = model.getBone("bomb1")
        val bomb2 = model.getBone("bomb2")
        val bomb3 = model.getBone("bomb3")
        val bomb4 = model.getBone("bomb4")
        val bomb5 = model.getBone("bomb5")

        bomb1.visible = !shouldHideBomb(vehicle, 4)
        bomb2.visible = !shouldHideBomb(vehicle, 3)
        bomb3.visible = !shouldHideBomb(vehicle, 2)
        bomb4.visible = !shouldHideBomb(vehicle, 1)
        bomb5.visible = !shouldHideBigBomb(vehicle, 1)

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