package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class M1A2Renderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 41.25) return 0F;
        if (t <= 47.25) return Mth.lerp((t - 41.25F) / (47.25F - 41.25F), 0F, -147.5F);
        if (t <= 53.9167) return -147.5F;
        if (t <= 54.5833) return Mth.lerp((t - 53.9167F) / (54.5833F - 53.9167F), -147.5F, -180F);
        if (t <= 85.5) return -180F;
        if (t <= 85.9167) return -180F;
        if (t <= 86.4167) return Mth.lerp((t - 85.9167F) / (86.4167F - 85.9167F), -180F, -205F);
        if (t <= 93.6667) return -205F;
        if (t <= 100) return Mth.lerp((t - 93.6667F) / (99.5F - 93.6667F), -205F, -360F);

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 41.75) return 0F;
        if (t <= 42.5833) return Mth.lerp((t - 41.75F) / (42.5833F - 41.75F), 0F, -0.72F);
        if (t <= 43.5) return Mth.lerp((t - 42.5833F) / (43.5F - 42.5833F), -0.72F, -2.175F);
        if (t <= 44.3333) return Mth.lerp((t - 43.5F) / (44.3333F - 43.5F), -2.175F, -5.01F);
        if (t <= 45.25) return Mth.lerp((t - 44.3333F) / (45.25F - 44.3333F), -5.01F, -7.8F);
        if (t <= 46.0833) return Mth.lerp((t - 45.25F) / (46.0833F - 45.25F), -7.8F, -10.6F);
        if (t <= 46.9167) return Mth.lerp((t - 46.0833F) / (46.9167F - 46.0833F), -10.6F, -13.245F);
        if (t <= 47.8333) return Mth.lerp((t - 46.9167F) / (47.8333F - 46.9167F), -13.245F, -14.81F);
        if (t <= 53.5833) return Mth.lerp((t - 47.8333F) / (53.5833F - 47.8333F), -14.81F, -24.64F);
        if (t <= 54.25) return Mth.lerp((t - 53.5833F) / (54.25F - 53.5833F), -24.64F, -25.39F);
        if (t <= 54.9167) return Mth.lerp((t - 54.25F) / (54.9167F - 54.25F), -25.39F, -25.74F);
        if (t <= 84.9167) return Mth.lerp((t - 54.9167F) / (84.9167F - 54.9167F), -25.74F, -25.73F);
        if (t <= 85.6667) return Mth.lerp((t - 84.9167F) / (85.6667F - 84.9167F), -25.73F, -25.33F);
        if (t <= 86.4167) return Mth.lerp((t - 85.6667F) / (86.4167F - 85.6667F), -25.33F, -24.72F);
        if (t <= 93.1667) return Mth.lerp((t - 86.4167F) / (93.1667F - 86.4167F), -24.72F, -15.75F);
        if (t <= 93.9167) return Mth.lerp((t - 93.1667F) / (93.9167F - 93.1667F), -15.75F, -14.53F);
        if (t <= 94.5833) return Mth.lerp((t - 93.9167F) / (94.5833F - 93.9167F), -14.53F, -12.57F);
        if (t <= 95.25) return Mth.lerp((t - 94.5833F) / (95.25F - 94.5833F), -12.57F, -10.62F);
        if (t <= 95.9167) return Mth.lerp((t - 95.25F) / (95.9167F - 95.25F), -10.62F, -8.48F);
        if (t <= 96.6667) return Mth.lerp((t - 95.9167F) / (96.6667F - 95.9167F), -8.48F, -6.2F);
        if (t <= 97.25) return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -6.2F, -4.35F);
        if (t <= 97.8333) return Mth.lerp((t - 97.25F) / (97.8333F - 97.25F), -4.35F, -2.68F);
        if (t <= 98.5) return Mth.lerp((t - 97.8333F) / (98.5F - 97.8333F), -2.68F, -1.26F);
        if (t <= 98.8333) return Mth.lerp((t - 98.5F) / (98.8333F - 98.5F), -1.26F, -0.68F);
        if (t <= 99.25) return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -0.68F, -0.3F);
        if (t <= 99.5833) return Mth.lerp((t - 99.25F) / (99.5833F - 99.25F), -0.3F, 0F);

        return 0F;
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 41.75) return Mth.lerp(t / (41.75F - 0F), 0F, 126.5F);
        if (t <= 42.5833) return Mth.lerp((t - 41.75F) / (42.5833F - 41.75F), 126.5F, 129.37F);
        if (t <= 43.5) return Mth.lerp((t - 42.5833F) / (43.5F - 42.5833F), 129.37F, 131.74F);
        if (t <= 44.3333) return Mth.lerp((t - 43.5F) / (44.3333F - 43.5F), 131.74F, 133.605F);
        if (t <= 45.25) return Mth.lerp((t - 44.3333F) / (45.25F - 44.3333F), 133.605F, 134.085F);
        if (t <= 46.0833) return Mth.lerp((t - 45.25F) / (46.0833F - 45.25F), 134.085F, 133.565F);
        if (t <= 46.9167) return Mth.lerp((t - 46.0833F) / (46.9167F - 46.0833F), 133.565F, 131.6F);
        if (t <= 47.8333) return Mth.lerp((t - 46.9167F) / (47.8333F - 46.9167F), 131.6F, 129.55F);
        if (t <= 53.5833) return Mth.lerp((t - 47.8333F) / (53.5833F - 47.8333F), 129.55F, 114.11F);
        if (t <= 54.25) return Mth.lerp((t - 53.5833F) / (54.25F - 53.5833F), 114.11F, 112.38F);
        if (t <= 54.9167) return Mth.lerp((t - 54.25F) / (54.9167F - 54.25F), 112.38F, 110.33F);
        if (t <= 84.9167) return Mth.lerp((t - 54.9167F) / (84.9167F - 54.9167F), 110.33F, 16.99F);
        if (t <= 85.6667) return Mth.lerp((t - 84.9167F) / (85.6667F - 84.9167F), 16.99F, 14.71F);
        if (t <= 86.4167) return Mth.lerp((t - 85.6667F) / (86.4167F - 85.6667F), 14.71F, 12.51F);
        if (t <= 93.1667) return Mth.lerp((t - 86.4167F) / (93.1667F - 86.4167F), 12.51F, -6.97F);
        if (t <= 93.9167) return Mth.lerp((t - 93.1667F) / (93.9167F - 93.1667F), -6.97F, -8.64F);
        if (t <= 94.5833) return Mth.lerp((t - 93.9167F) / (94.5833F - 93.9167F), -8.64F, -10.07F);
        if (t <= 95.25) return Mth.lerp((t - 94.5833F) / (95.25F - 94.5833F), -10.07F, -10.86F);
        if (t <= 95.9167) return Mth.lerp((t - 95.25F) / (95.9167F - 95.25F), -10.86F, -11.18F);
        if (t <= 96.6667) return Mth.lerp((t - 95.9167F) / (96.6667F - 95.9167F), -11.18F, -10.8F);
        if (t <= 97.25) return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -10.8F, -10.05F);
        if (t <= 97.8333) return Mth.lerp((t - 97.25F) / (97.8333F - 97.25F), -10.05F, -8.695F);
        if (t <= 98.5) return Mth.lerp((t - 97.8333F) / (98.5F - 97.8333F), -8.695F, -6.38F);
        if (t <= 98.8333) return Mth.lerp((t - 98.5F) / (98.8333F - 98.5F), -6.38F, -5.01F);
        if (t <= 99.25) return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -5.01F, -3.39F);
        if (t <= 99.5833) return Mth.lerp((t - 99.25F) / (99.5833F - 99.25F), -3.39F, -1.69F);

        return Mth.lerp((t - 99.5833F) / (100F - 99.5833F), -1.69F, 0F);
    }

    override fun getTrackDistance(): Float {
        return 2.27f
    }
}
