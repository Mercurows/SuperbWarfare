package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.KirovEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class KirovRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : KirovEntity, T : BasicGeoVehicleEntity {

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

        val propeller = model.getBone("prop1")
        val propeller2 = model.getBone("prop2")
        val propeller3 = model.getBone("prop3")

        val propeller4 = model.getBone("prop4")
        val propeller5 = model.getBone("prop5")

        val rot = Mth.lerp(partialTicks, vehicle.propellerO, vehicle.propeller)
        propeller.rotation.rotateZ(rot)

        val rotL = Mth.lerp(partialTicks, vehicle.propellerLO, vehicle.propellerL)
        propeller2.rotation.rotateZ(rotL)

        val rotR = Mth.lerp(partialTicks, vehicle.propellerRO, vehicle.propellerR)
        propeller3.rotation.rotateZ(rotR)

        val rotV = Mth.lerp(partialTicks, vehicle.propellerVO, vehicle.propellerV)

        propeller4.rotation.rotateZ(rotV)
        propeller5.rotation.rotateZ(rotV)

        val turretRight = model.getBone("turret_right")
        if (turretRight != null) {
            turretRight.rotation.rotationY(turretYRot * Mth.DEG_TO_RAD)
            turretRight.visible = !(vehicle.isWreck && vehicle.hasTurret() && vehicle.sympatheticDetonated)
        }

        val controlP = model.getBone("controlP")
        controlP?.rotation?.rotationX(Mth.clamp(-vehicle.power * 48, -20f, 20f) * Mth.DEG_TO_RAD)

        val rudder = model.getBone("rudder")
        rudder.rotation.rotationZ(12 * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
    }
}
