package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class SpeedBoatRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val propeller = model.getBone("propeller")
        val propeller2 = model.getBone("propeller2")
        val turret = model.getBone("turret")
        val control = model.getBone("control")
        val rudder = model.getBone("rudder")

        propeller.rotation.rotationZ(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        propeller2.rotation.rotationZ(-Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        turret.visible = !(vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer && ClientEventHandler.zoomVehicle)
        control.rotation.rotationZ(-4 * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
        rudder.rotation.rotationY(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
    }
}
