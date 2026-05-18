package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

open class WheelChairRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    override fun customModelPart(entityIn: T, model: BedrockModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        val wRb = model.getBone("w_rb")
        val wLb = model.getBone("w_lb")
        val wRr = model.getBone("w_rr")
        val wLr = model.getBone("w_lr")

        wRb.rotation.rotationX(Mth.lerp(partialTicks, entityIn.rightWheelRotO, entityIn.rightWheelRot))
        wLb.rotation.rotationX(Mth.lerp(partialTicks, entityIn.leftWheelRotO, entityIn.leftWheelRot))
        wRr.rotation.rotationX(4 * Mth.lerp(partialTicks, entityIn.rightWheelRotO, entityIn.rightWheelRot))
        wLr.rotation.rotationX(4 * Mth.lerp(partialTicks, entityIn.leftWheelRotO, entityIn.leftWheelRot))
    }
}