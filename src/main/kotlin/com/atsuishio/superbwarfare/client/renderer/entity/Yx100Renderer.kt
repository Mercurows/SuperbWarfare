package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class Yx100Renderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun renderScale(): Float {
        return 1.25f
    }

    override fun renderCustomPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(ModRenderTypes.LASER.apply(MUZZLE_GLOW)),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 34.75) return 0f
        if (t <= 35.5) return Mth.lerp((t - 34.75f) / (35.5f - 34.75f), 0f, -45f)
        if (t <= 35.8333) return -45f
        if (t <= 36.5) return Mth.lerp((t - 35.8333f) / (36.5f - 35.8333f), -45f, -90f)
        if (t <= 36.6667) return -90f
        if (t <= 37) return Mth.lerp((t - 36.6667f) / (37f - 36.6667f), -90f, -112.5f)
        if (t <= 37.3333) return -112.5f
        if (t <= 37.5) return -112.5f
        if (t <= 38.1667) return Mth.lerp((t - 37.5f) / (38.1667f - 37.5f), -112.5f, -135f)
        if (t <= 41.9167) return -135f
        if (t <= 42.4167) return Mth.lerp((t - 41.9167f) / (42.4167f - 41.9167f), -135f, -157.5f)
        if (t <= 43.1667) return -157.5f
        if (t <= 43.6667) return Mth.lerp((t - 43.1667f) / (43.6667f - 43.1667f), -157.5f, -180f)
        if (t <= 68) return -180f
        if (t <= 68.5) return Mth.lerp((t - 68f) / (68.5f - 68f), -180f, -202.5f)
        if (t <= 69.25) return -202.5f
        if (t <= 69.8333) return Mth.lerp((t - 69.25f) / (69.8333f - 69.25f), -202.5f, -220f)
        if (t <= 73.5) return -220f
        if (t <= 74.1667) return Mth.lerp((t - 73.5f) / (74.1667f - 73.5f), -220f, -242.5f)
        if (t <= 75.6667) return -242.5f
        if (t <= 76.1667) return Mth.lerp((t - 75.6667f) / (76.1667f - 75.6667f), -242.5f, -295f)
        if (t <= 76.6667) return -295f
        if (t <= 77.1667) return Mth.lerp((t - 76.6667f) / (77.1667f - 76.6667f), -295f, -340f)
        if (t <= 77.8333) return Mth.lerp((t - 77.1667f) / (77.8333f - 77.1667f), -340f, -360f)
        if (t <= 79.5) return -360f

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 35.1667) return 0f
        if (t <= 36.1667) return Mth.lerp(t - 35.1667f, 0f, -2.91f)
        if (t <= 37) return Mth.lerp((t - 36.1667f) / (37f - 36.1667f), -2.91f, -6.79f)
        if (t <= 37.8333) return Mth.lerp((t - 37f) / (37.8333f - 37f), -6.79f, -10.005f)
        if (t <= 42.1667) return Mth.lerp((t - 37.8333f) / (42.1667f - 37.8333f), -10.005f, -22.38f)
        if (t <= 43.4167) return Mth.lerp((t - 42.1667f) / (43.4167f - 42.1667f), -22.38f, -24.14f)
        if (t <= 68.25) return -24.14f
        if (t <= 69.5) return Mth.lerp((t - 68.25f) / (69.5f - 68.25f), -24.14f, -22.45f)
        if (t <= 73.8333) return Mth.lerp((t - 69.5f) / (73.8333f - 69.5f), -22.45f, -11.12f)
        if (t <= 75.9167) return Mth.lerp((t - 73.8333f) / (75.9167f - 73.8333f), -11.12f, -4.155f)
        if (t <= 76.9167) return Mth.lerp(t - 75.9167f, -4.155f, -0.855f)
        if (t <= 78.0833) return Mth.lerp((t - 76.9167f) / (78.0833f - 76.9167f), -0.855f, 0f)

        return Mth.lerp((t - 79.25f) / (80f - 79.25f), -0.025f, 0f)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 35.1667) return Mth.lerp(t / (35.1667f - 0f), 0f, 121.385f)
        if (t <= 36.1667) return Mth.lerp(t - 35.1667f, 121.385f, 124.37f)
        if (t <= 37) return 124.37f
        if (t <= 37.8333) return Mth.lerp((t - 37f) / (37.8333f - 37f), 124.37f, 122.73f)
        if (t <= 42.1667) return Mth.lerp((t - 37.8333f) / (42.1667f - 37.8333f), 122.73f, 110.455f)
        if (t <= 43.4167) return Mth.lerp((t - 42.1667f) / (43.4167f - 42.1667f), 110.455f, 105.805f)
        if (t <= 68.25) return Mth.lerp((t - 43.4167f) / (68.25f - 43.4167f), 105.805f, 10.09f)
        if (t <= 69.5) return Mth.lerp((t - 68.25f) / (69.5f - 68.25f), 10.09f, 5.625f)
        if (t <= 73.8333) return Mth.lerp((t - 69.5f) / (73.8333f - 69.5f), 5.625f, -8.025f)
        if (t <= 75.9167) return Mth.lerp((t - 73.8333f) / (75.9167f - 73.8333f), -8.025f, -11.175f)
        if (t <= 76.9167) return Mth.lerp(t - 75.9167f, -11.175f, -9.35f)
        if (t <= 78.0833) return Mth.lerp((t - 76.9167f) / (78.0833f - 76.9167f), -9.35f, -5.38f)

        return Mth.lerp((t - 79.25f) / (80f - 79.25f), -4.12f, 0f)
    }

    override fun getTrackDistance(): Float {
        return 1.96f
    }

    companion object {
        val MUZZLE_GLOW = Mod.loc("textures/bedrock/vehicle/yx_100_glow.png")
    }
}
