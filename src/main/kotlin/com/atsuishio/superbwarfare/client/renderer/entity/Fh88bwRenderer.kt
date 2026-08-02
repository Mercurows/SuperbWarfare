package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Fh88bwRenderer(manager: EntityRendererProvider.Context) : BasicArtilleryRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        entity: ArtilleryEntity,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks)

        val pitch = Mth.clamp(-turretXRot, entity.turretMinPitch, entity.turretMaxPitch) * Mth.DEG_TO_RAD

        val barrel = instance.getBone("barrel")
        val angle = if (!entity.lockTurret) {
            pitch
        } else {
            0f
        }

        barrel?.rotation?.rotationX(angle)

        val b = atan2(11.8113, -14.0761) -
                atan2(
                    32.1847 * sin(pitch) + 9.4012 * cos(pitch) + 2.4101,
                    -32.1847 * cos(pitch) + 9.4012 * sin(pitch) + 18.1086
                )

        instance.getBone("move_yeyagan")?.rotation?.rotationX(b.toFloat())
        instance.getBone("move_yeya")?.rotation?.rotationX((b - angle).toFloat())
        instance.getBone("move_control")?.rotation?.rotationY(12 * Mth.lerp(partialTicks, entity.rudderRotO, entity.rudderRot))

        instance.getBone("move_hmg")?.visible =
            !(localPlayer == entity.getNthEntity(2) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle))
    }

    override fun tickVariables(entity: ArtilleryEntity, entityYaw: Float, partialTicks: Float) {
        super.tickVariables(entity, entityYaw, partialTicks)
        hideFlare = localPlayer == entity.getNthEntity(2) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)
    }
}
