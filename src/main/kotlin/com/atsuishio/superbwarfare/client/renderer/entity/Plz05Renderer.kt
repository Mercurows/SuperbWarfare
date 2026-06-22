package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Plz05Renderer(manager: EntityRendererProvider.Context) : BasicArtilleryRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        vehicle: ArtilleryEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val tiTop1 = model.getBone("titop1")
        tiTop1.visible = !(vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer && options.cameraType == CameraType.FIRST_PERSON)

        val barrel = model.getBone("barrel")
        val angle = if (!vehicle.lockTurret) {
            Mth.clamp(-turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Mth.DEG_TO_RAD
        } else {
            1.2f * Mth.DEG_TO_RAD
        }

        barrel.rotation.rotationX(angle)
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 43.6667) return 0f
        if (t <= 44.3333) return Mth.lerp((t - 43.6667f) / (44.3333f - 43.6667f), 0f, -45f)
        if (t <= 45.6667) return Mth.lerp((t - 44.3333f) / (45.6667f - 44.3333f), -45f, -67.5f)
        if (t <= 46.3333) return Mth.lerp((t - 45.6667f) / (46.3333f - 45.6667f), -67.5f, -90f)
        if (t <= 47.6667) return -90f
        if (t <= 48.3333) return Mth.lerp((t - 47.6667f) / (48.3333f - 47.6667f), -90f, -135f)
        if (t <= 49.3333) return Mth.lerp(t - 48.3333f, -135f, -145f)
        if (t <= 50) return Mth.lerp((t - 49.3333f) / (50f - 49.3333f), -145f, -154.5f)
        if (t <= 54.6667) return -154.5f
        if (t <= 55.3333) return Mth.lerp((t - 54.6667f) / (55.3333f - 54.6667f), -154.5f, -180f)
        if (t <= 86.4167) return -180f
        if (t <= 87.0833) return Mth.lerp((t - 86.4167f) / (87.0833f - 86.4167f), -180f, -200f)
        if (t <= 95) return Mth.lerp((t - 87.0833f) / (95f - 87.0833f), -200f, -210f)
        if (t <= 95.6667) return Mth.lerp((t - 95f) / (95.6667f - 95f), -210f, -225f)
        if (t <= 96.3333) return Mth.lerp((t - 95.6667f) / (96.3333f - 95.6667f), -225f, -247.5f)
        if (t <= 97) return Mth.lerp((t - 96.3333f) / (97f - 96.3333f), -247.5f, -270f)
        if (t <= 98) return Mth.lerp(t - 97f, -270f, -272.5f)
        if (t <= 98.6667) return Mth.lerp((t - 98f) / (98.6667f - 98f), -272.5f, -315f)
        if (t <= 99.6667) return Mth.lerp(t - 98.6667f, -315f, -337.5f)
        if (t <= 99.9999) return Mth.lerp((t - 99.6667f) / (100f - 99.6667f), -337.5f, -360f)

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 44) return Mth.lerp(t / 44f, 0f, -0.6f)
        if (t <= 46) return Mth.lerp((t - 44f) / (46f - 44f), -0.6f, -4.13f)
        if (t <= 48) return Mth.lerp((t - 46f) / (48f - 46f), -4.13f, -9.565f)
        if (t <= 49.5833) return Mth.lerp((t - 48f) / (49.5833f - 48f), -9.565f, -12.32f)
        if (t <= 55) return Mth.lerp((t - 49.5833f) / (55f - 49.5833f), -12.32f, -19.71f)
        if (t <= 86.75) return Mth.lerp((t - 55f) / (86.75f - 55f), -19.71f, -19.67f)
        if (t <= 95.3333) return Mth.lerp((t - 86.75f) / (95.3333f - 86.75f), -19.67f, -11.005f)
        if (t <= 96.6667) return Mth.lerp((t - 95.3333f) / (96.6667f - 95.3333f), -11.005f, -8.35f)
        if (t <= 98.3333) return Mth.lerp((t - 96.6667f) / (98.3333f - 96.6667f), -8.35f, -3.285f)

        return Mth.lerp((t - 98.3333f) / (100f - 98.3333f), -3.285f, 0f)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 44) return Mth.lerp(t / 44f, 0f, 133.75f)
        if (t <= 46) return Mth.lerp((t - 44f) / (46f - 44f), 133.75f, 137.405f)
        if (t <= 48) return Mth.lerp((t - 46f) / (48f - 46f), 137.405f, 137.47f)
        if (t <= 49.5833) return Mth.lerp((t - 48f) / (49.5833f - 48f), 137.47f, 134.72f)
        if (t <= 55) return Mth.lerp((t - 49.5833f) / (55f - 49.5833f), 134.72f, 119.36f)
        if (t <= 86.75) return Mth.lerp((t - 55f) / (86.75f - 55f), 119.36f, 23.32f)
        if (t <= 95.3333) return Mth.lerp((t - 86.75f) / (95.3333f - 86.75f), 23.32f, -0.695f)
        if (t <= 96.6667) return Mth.lerp((t - 95.3333f) / (96.6667f - 95.3333f), -0.695f, -3.205f)
        if (t <= 98.3333) return Mth.lerp((t - 96.6667f) / (98.3333f - 96.6667f), -3.205f, -3.28f)

        return Mth.lerp((t - 98.3333f) / (100f - 98.3333f), -3.28f, 0f)
    }
}