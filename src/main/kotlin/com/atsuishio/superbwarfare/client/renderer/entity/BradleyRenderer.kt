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

class BradleyRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
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

        val guangDian = model.getBone("guangdian")
        val rot = Mth.clamp(-turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Mth.DEG_TO_RAD
        guangDian.rotation.rotationX(rot)

        val player = localPlayer
        val hide = player != null && vehicle === player.vehicle && vehicle.getFirstPassenger() !== player && vehicle.hasWeapon(vehicle.getSeatIndex(player)) && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)

        val base = model.getBone("base")
        val track = model.getBone("Track")

        base.visible = !hide
        track.visible = !hide
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 40.8333) return 0F
        if (t <= 48.1667) return Mth.lerp((t - 40.8333F) / (48.1667F - 40.8333F), 0F, -159.5F)
        if (t <= 55.25) return -159.5F
        if (t <= 57.25) return Mth.lerp((t - 55.25F) / (57.25F - 55.25F), -159.5F, -180F)
        if (t <= 85.5) return -180F
        if (t <= 87) return Mth.lerp((t - 85.5F) / (87F - 85.5F), -180F, -200F)
        if (t <= 92.4167) return -200F
        if (t <= 100) return Mth.lerp((t - 92.4167F) / (100F - 92.4167F), -200F, -360F)

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 40.8333) return 0F
        if (t <= 41.4167) return Mth.lerp((t - 40.8333F) / (41.4167F - 40.8333F), 0F, -0.215F)
        if (t <= 41.9167) return Mth.lerp((t - 41.4167F) / (41.9167F - 41.4167F), -0.215F, -0.52F)
        if (t <= 42.4167) return Mth.lerp((t - 41.9167F) / (42.4167F - 41.9167F), -0.52F, -0.9F)
        if (t <= 42.9167) return Mth.lerp((t - 42.4167F) / (42.9167F - 42.4167F), -0.9F, -1.44F)
        if (t <= 43.4167) return Mth.lerp((t - 42.9167F) / (43.4167F - 42.9167F), -1.44F, -2.3F)
        if (t <= 43.8333) return Mth.lerp((t - 43.4167F) / (43.8333F - 43.4167F), -2.3F, -3.21F)
        if (t <= 44.25) return Mth.lerp((t - 43.8333F) / (44.25F - 43.8333F), -3.21F, -4.22F)
        if (t <= 44.75) return Mth.lerp((t - 44.25F) / (44.75F - 44.25F), -4.22F, -5.505F)
        if (t <= 45.25) return Mth.lerp((t - 44.75F) / (45.25F - 44.75F), -5.505F, -6.79F)
        if (t <= 45.75) return Mth.lerp((t - 45.25F) / (45.75F - 45.25F), -6.79F, -8.08F)
        if (t <= 46.1667) return Mth.lerp((t - 45.75F) / (46.1667F - 45.75F), -8.08F, -9.09F)
        if (t <= 46.6667) return Mth.lerp((t - 46.1667F) / (46.6667F - 46.1667F), -9.09F, -10.305F)
        if (t <= 47.0833) return Mth.lerp((t - 46.6667F) / (47.0833F - 46.6667F), -10.305F, -11.025F)
        if (t <= 47.5) return Mth.lerp((t - 47.0833F) / (47.5F - 47.0833F), -11.025F, -11.69F)
        if (t <= 47.9167) return Mth.lerp((t - 47.5F) / (47.9167F - 47.5F), -11.69F, -12.3F)
        if (t <= 48.4167) return Mth.lerp((t - 47.9167F) / (48.4167F - 47.9167F), -12.3F, -12.935F)
        if (t <= 49.3333) return Mth.lerp((t - 48.4167F) / (49.3333F - 48.4167F), -12.935F, -13.83F)
        if (t <= 55.25) return Mth.lerp((t - 49.3333F) / (55.25F - 49.3333F), -13.83F, -19.27F)
        if (t <= 56.25) return Mth.lerp(t - 55.25F, -19.27F, -19.895F)
        if (t <= 57.25) return Mth.lerp(t - 56.25F, -19.895F, -20.2F)
        if (t <= 85.5) return -20.2F
        if (t <= 86.25) return Mth.lerp((t - 85.5F) / (86.25F - 85.5F), -20.2F, -20.1F)
        if (t <= 87) return Mth.lerp((t - 86.25F) / (87F - 86.25F), -20.1F, -19.55F)
        if (t <= 92.4167) return Mth.lerp((t - 87F) / (92.4167F - 87F), -19.55F, -14.75F)
        if (t <= 93.3333) return Mth.lerp((t - 92.4167F) / (93.3333F - 92.4167F), -14.75F, -13.475F)
        if (t <= 93.9167) return Mth.lerp((t - 93.3333F) / (93.9167F - 93.3333F), -13.475F, -12.24F)
        if (t <= 94.4167) return Mth.lerp((t - 93.9167F) / (94.4167F - 93.9167F), -12.24F, -10.9F)
        if (t <= 94.9167) return Mth.lerp((t - 94.4167F) / (94.9167F - 94.4167F), -10.9F, -9.62F)
        if (t <= 95.5) return Mth.lerp((t - 94.9167F) / (95.5F - 94.9167F), -9.62F, -8.13F)
        if (t <= 96.0833) return Mth.lerp((t - 95.5F) / (96.0833F - 95.5F), -8.13F, -6.56F)
        if (t <= 96.6667) return Mth.lerp((t - 96.0833F) / (96.6667F - 96.0833F), -6.56F, -4.99F)
        if (t <= 97.25) return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -4.99F, -3.655F)
        if (t <= 97.75) return Mth.lerp((t - 97.25F) / (97.75F - 97.25F), -3.655F, -2.65F)
        if (t <= 98.1667) return Mth.lerp((t - 97.75F) / (98.1667F - 97.75F), -2.65F, -1.655F)
        if (t <= 98.5833) return Mth.lerp((t - 98.1667F) / (98.5833F - 98.1667F), -1.655F, -0.93F)
        if (t <= 99) return Mth.lerp((t - 98.5833F) / (99F - 98.5833F), -0.93F, -0.415F)
        if (t <= 99.4167) return Mth.lerp((t - 99F) / (99.4167F - 99F), -0.415F, -0.14F)

        return Mth.lerp((t - 99.4167F) / (100F - 99.4167F), -0.14F, 0F)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 40.8333) return Mth.lerp(t / (40.8333F - 0F), 0F, 106F)
        if (t <= 41.4167) return Mth.lerp((t - 40.8333F) / (41.4167F - 40.8333F), 106F, 107.3F)
        if (t <= 41.9167) return Mth.lerp((t - 41.4167F) / (41.9167F - 41.4167F), 107.3F, 108.41F)
        if (t <= 42.4167) return Mth.lerp((t - 41.9167F) / (42.4167F - 41.9167F), 108.41F, 109.39F)
        if (t <= 42.9167) return Mth.lerp((t - 42.4167F) / (42.9167F - 42.4167F), 109.39F, 110.36F)
        if (t <= 43.4167) return Mth.lerp((t - 42.9167F) / (43.4167F - 42.9167F), 110.36F, 111.365F)
        if (t <= 43.8333) return Mth.lerp((t - 43.4167F) / (43.8333F - 43.4167F), 111.365F, 112.03F)
        if (t <= 44.25) return Mth.lerp((t - 43.8333F) / (44.25F - 43.8333F), 112.03F, 112.58F)
        if (t <= 44.75) return Mth.lerp((t - 44.25F) / (44.75F - 44.25F), 112.58F, 112.91F)
        if (t <= 45.25) return Mth.lerp((t - 44.75F) / (45.25F - 44.75F), 112.91F, 112.955F)
        if (t <= 45.75) return Mth.lerp((t - 45.25F) / (45.75F - 45.25F), 112.955F, 112.795F)
        if (t <= 46.1667) return Mth.lerp((t - 45.75F) / (46.1667F - 45.75F), 112.795F, 112.515F)
        if (t <= 46.6667) return Mth.lerp((t - 46.1667F) / (46.6667F - 46.1667F), 112.515F, 111.885F)
        if (t <= 47.0833) return Mth.lerp((t - 46.6667F) / (47.0833F - 46.6667F), 111.885F, 111.2F)
        if (t <= 47.5) return Mth.lerp((t - 47.0833F) / (47.5F - 47.0833F), 111.2F, 110.21F)
        if (t <= 47.9167) return Mth.lerp((t - 47.5F) / (47.9167F - 47.5F), 110.21F, 109.205F)
        if (t <= 48.4167) return Mth.lerp((t - 47.9167F) / (48.4167F - 47.9167F), 109.205F, 107.88F)
        if (t <= 49.3333) return Mth.lerp((t - 48.4167F) / (49.3333F - 48.4167F), 107.88F, 105.66F)
        if (t <= 55.25) return Mth.lerp((t - 49.3333F) / (55.25F - 49.3333F), 105.66F, 91.02F)
        if (t <= 56.25) return Mth.lerp(t - 55.25F, 91.02F, 88.545F)
        if (t <= 57.25) return Mth.lerp(t - 56.25F, 88.545F, 85.93F)
        if (t <= 85.5) return Mth.lerp((t - 57.25F) / (85.5F - 57.25F), 85.93F, 11.91F)
        if (t <= 86.25) return Mth.lerp((t - 85.5F) / (86.25F - 85.5F), 11.91F, 10F)
        if (t <= 87) return Mth.lerp((t - 86.25F) / (87F - 86.25F), 10F, 8.16F)
        if (t <= 92.4167) return Mth.lerp((t - 87F) / (92.4167F - 87F), 8.16F, -5.15F)
        if (t <= 93.3333) return Mth.lerp((t - 92.4167F) / (93.3333F - 92.4167F), -5.15F, -7.4F)
        if (t <= 93.9167) return Mth.lerp((t - 93.3333F) / (93.9167F - 93.3333F), -7.4F, -8.41F)
        if (t <= 94.4167) return Mth.lerp((t - 93.9167F) / (94.4167F - 93.9167F), -8.41F, -8.99F)
        if (t <= 94.9167) return Mth.lerp((t - 94.4167F) / (94.9167F - 94.4167F), -8.99F, -9.515F)
        if (t <= 95.5) return Mth.lerp((t - 94.9167F) / (95.5F - 94.9167F), -9.515F, -9.69F)
        if (t <= 96.0833) return Mth.lerp((t - 95.5F) / (96.0833F - 95.5F), -9.69F, -9.515F)
        if (t <= 96.6667) return Mth.lerp((t - 96.0833F) / (96.6667F - 96.0833F), -9.515F, -8.99F)
        if (t <= 97.25) return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -8.99F, -8.305F)
        if (t <= 97.75) return Mth.lerp((t - 97.25F) / (97.75F - 97.25F), -8.305F, -7.385F)
        if (t <= 98.1667) return Mth.lerp((t - 97.75F) / (98.1667F - 97.75F), -7.385F, -6.19F)
        if (t <= 98.5833) return Mth.lerp((t - 98.1667F) / (98.5833F - 98.1667F), -6.19F, -4.79F)
        if (t <= 99) return Mth.lerp((t - 98.5833F) / (99F - 98.5833F), -4.79F, -3.39F)
        if (t <= 99.4167) return Mth.lerp((t - 99F) / (99.4167F - 99F), -3.39F, -1.99F)

        return Mth.lerp((t - 99.4167F) / (100F - 99.4167F), -1.99F, 0F)
    }
}
