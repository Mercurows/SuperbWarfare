package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Bmp2Renderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun renderScale(): Float {
        return 0.9f
    }

    override fun transformCustomModelPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val player = localPlayer
        val hide = player != null && vehicle === player.vehicle && vehicle.getFirstPassenger() !== player && vehicle.hasWeapon(vehicle.getSeatIndex(player)) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)

        val base = model.getBone("base")
        val track = model.getBone("Track")

        base.visible = !hide
        track.visible = !hide
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 37.6667) return 0f
        if (t <= 38.5833) return Mth.lerp((t - 37.6667f) / (38.5833f - 37.6667f), 0f, -45f)
        if (t <= 39.75) return -45f
        if (t <= 40.6667) return Mth.lerp((t - 39.75f) / (40.6667f - 39.75f), -45f, -90f)
        if (t <= 41.6667) return -90f
        if (t <= 42.5) return -90f
        if (t <= 43.5) return Mth.lerp(t - 42.5f, -90f, -135f)
        if (t <= 44.5833) return -135f
        if (t <= 45.0833) return Mth.lerp((t - 44.5833f) / (45.0833f - 44.5833f), -135f, -150f)
        if (t <= 52.25) return -150f
        if (t <= 52.75) return Mth.lerp((t - 52.25f) / (52.75f - 52.25f), -150f, -180f)
        if (t <= 84.3333) return -180f
        if (t <= 84.9167) return Mth.lerp((t - 84.3333f) / (84.9167f - 84.3333f), -180f, -210f)
        if (t <= 92.5833) return -210f
        if (t <= 93.4167) return Mth.lerp((t - 92.5833f) / (93.4167f - 92.5833f), -210f, -220f)
        if (t <= 94.25) return -220f
        if (t <= 94.9167) return Mth.lerp((t - 94.25f) / (94.9167f - 94.25f), -220f, -243.33f)
        if (t <= 95.75) return Mth.lerp((t - 94.9167f) / (95.75f - 94.9167f), -243.33f, -270f)
        if (t <= 96.8333) return -270f
        if (t <= 97.5833) return Mth.lerp((t - 96.8333f) / (97.5833f - 96.8333f), -270f, -315f)
        if (t <= 98.8333) return -315f
        if (t <= 99.5833) return Mth.lerp((t - 98.8333f) / (99.5833f - 98.8333f), -315f, -360f)

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 37.6667) return 0f
        if (t <= 38.5833) return Mth.lerp((t - 37.6667f) / (38.5833f - 37.6667f), 0f, -1.8f)
        if (t <= 40.3333) return Mth.lerp((t - 38.5833f) / (40.3333f - 38.5833f), -1.8f, -4.1f)
        if (t <= 42.9167) return Mth.lerp((t - 40.3333f) / (42.9167f - 40.3333f), -4.1f, -10.3f)
        if (t <= 44.25) return Mth.lerp((t - 42.9167f) / (44.25f - 42.9167f), -10.3f, -12.9f)
        if (t <= 52.4167) return Mth.lerp((t - 44.25f) / (52.4167f - 44.25f), -12.9f, -23.96f)
        if (t <= 84.5833) return -23.96f
        if (t <= 93) return Mth.lerp((t - 84.5833f) / (93f - 84.5833f), -23.96f, -12.93f)
        if (t <= 95.25) return Mth.lerp((t - 93f) / (95.25f - 93f), -12.93f, -10.085f)
        if (t <= 97.5) return Mth.lerp((t - 95.25f) / (97.5f - 95.25f), -10.085f, -4.585f)
        if (t <= 98.8333) return Mth.lerp((t - 97.5f) / (98.8333f - 97.5f), -4.585f, -1.165f)
        if (t <= 99.25) return Mth.lerp((t - 98.8333f) / (99.25f - 98.8333f), -1.165f, -0.25f)

        return Mth.lerp((t - 99.25f) / (100f - 99.25f), -0.25f, 0f)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 37.6667) return Mth.lerp(t / (37.6667f - 0f), 0f, 111.6f)
        if (t <= 38.5833) return Mth.lerp((t - 37.6667f) / (38.5833f - 37.6667f), 111.6f, 113.25f)
        if (t <= 40.3333) return Mth.lerp((t - 38.5833f) / (40.3333f - 38.5833f), 113.25f, 116f)
        if (t <= 42.9167) return 116f
        if (t <= 44.25) return Mth.lerp((t - 42.9167f) / (44.25f - 42.9167f), 116f, 113.5f)
        if (t <= 52.4167) return Mth.lerp((t - 44.25f) / (52.4167f - 44.25f), 113.5f, 96.25f)
        if (t <= 84.5833) return Mth.lerp((t - 52.4167f) / (84.5833f - 52.4167f), 96.25f, 14.095f)
        if (t <= 93) return Mth.lerp((t - 84.5833f) / (93f - 84.5833f), 14.095f, -3.565f)
        if (t <= 95.25) return Mth.lerp((t - 93f) / (95.25f - 93f), -3.565f, -6.35f)
        if (t <= 97.5) return Mth.lerp((t - 95.25f) / (97.5f - 95.25f), -6.35f, -6.39f)
        if (t <= 98.8333) return Mth.lerp((t - 97.5f) / (98.8333f - 97.5f), -6.39f, -3.03f)
        if (t <= 99.25) return Mth.lerp((t - 98.8333f) / (99.25f - 98.8333f), -3.03f, -1.95f)

        return Mth.lerp((t - 99.25f) / (100f - 99.25f), -1.95f, 0f)
    }
}
