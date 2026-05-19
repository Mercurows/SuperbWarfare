package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpTowEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import org.joml.Quaterniond
import org.joml.Quaternionf

class SodayoPickUpRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(vehicle: T, model: BedrockVehicleModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
        val control = model.getBone("control")
        val head = model.getBone("head")

        control.rotation.rotationZ(8 * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))

        val pitchRot = Axis.XP.rotation(head.rotationInEuler.x + -5f * vehicle.getAcceleration().toFloat())
        val rollRot = Axis.ZP.rotation(head.rotationInEuler.z + 0.5f * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot) * vehicle.deltaMovement.horizontalDistance().toFloat())
        val quaternion =  Quaterniond(pitchRot).mul(Quaterniond(rollRot))
        head.rotation.mul(Quaternionf(quaternion))

        if (vehicle is SodayoPickUpRocketEntity) {
            // TODO 正确实现隐藏火箭弹
//            model.shell.forEach {
//                val items = vehicle.getEntityData().get(SodayoPickUpRocketEntity.LOADED_AMMO)
//                val i = matcher.group("id").toInt()
//                it.visible = items[i] != -1
//            }
        }

        if (vehicle is SodayoPickUpTowEntity) {
            val guanMiao = model.getBone("guanmiao")
            guanMiao.visible = !(vehicle.turretControllerIndex == vehicle.getSeatIndex(localPlayer) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle))
        }
    }
}


