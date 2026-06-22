package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class T90aRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun renderScale(): Float {
        return 1.1f
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 42.3333) return -1.25F
        if (t <= 47.6667) return Mth.lerp((t - 42.3333F) / (47.6667F - 42.3333F), -1.25F, -135F)
        if (t <= 48.6667) return -135F
        if (t <= 49.3333) return Mth.lerp((t - 48.6667F) / (49.3333F - 48.6667F), -135F, -145.4F)
        if (t <= 50) return Mth.lerp((t - 49.3333F) / (50F - 49.3333F), -145.4F, -155F)
        if (t <= 54) return -155F
        if (t <= 56.6667) return Mth.lerp((t - 54F) / (56.6667F - 54F), -155F, -180F)
        if (t <= 87) return -180F
        if (t <= 88.6667) return Mth.lerp((t - 87F) / (88.6667F - 87F), -180F, -216F)
        if (t <= 93.8333) return -216F
        if (t <= 94.1667) return Mth.lerp((t - 93.8333F) / (94.1667F - 93.8333F), -216F, -218.5F)
        if (t <= 94.5) return Mth.lerp((t - 94.1667F) / (94.5F - 94.1667F), -218.5F, -221F)
        if (t <= 95.5) return -221F
        if (t <= 100) return Mth.lerp((t - 95.5F) / (100F - 95.5F), -221F, -360F)

        return 0f
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 43) return Mth.lerp(t / 43F, 0.1F, -2.35F)
        if (t <= 44) return Mth.lerp(t - 43F, -2.35F, -3.46F)
        if (t <= 44.8333) return Mth.lerp((t - 44F) / (44.8333F - 44F), -3.46F, -5.665F)
        if (t <= 45.75) return Mth.lerp((t - 44.8333F) / (45.75F - 44.8333F), -5.665F, -7.72F)
        if (t <= 46.75) return Mth.lerp(t - 45.75F, -7.72F, -9.965F)
        if (t <= 48) return Mth.lerp((t - 46.75F) / (48F - 46.75F), -9.965F, -12.62F)
        if (t <= 49.25) return Mth.lerp((t - 48F) / (49.25F - 48F), -12.62F, -14.57F)
        if (t <= 54) return Mth.lerp((t - 49.25F) / (54F - 49.25F), -14.57F, -19.92F)
        if (t <= 55.1667) return Mth.lerp((t - 54F) / (55.1667F - 54F), -19.92F, -20.81F)
        if (t <= 56.6667) return Mth.lerp((t - 55.1667F) / (56.6667F - 55.1667F), -20.81F, -20.92F)
        if (t <= 86.6667) return Mth.lerp((t - 56.6667F) / (86.6667F - 56.6667F), -20.92F, -21.02F)
        if (t <= 87.1667) return Mth.lerp((t - 86.6667F) / (87.1667F - 86.6667F), -21.02F, -21.05F)
        if (t <= 87.75) return Mth.lerp((t - 87.1667F) / (87.75F - 87.1667F), -21.05F, -20.77F)
        if (t <= 88.3333) return Mth.lerp((t - 87.75F) / (88.3333F - 87.75F), -20.77F, -20.09F)
        if (t <= 88.8333) return Mth.lerp((t - 88.3333F) / (88.8333F - 88.3333F), -20.09F, -19.32F)
        if (t <= 94.0833) return Mth.lerp((t - 88.8333F) / (94.0833F - 88.8333F), -19.32F, -11.055F)
        if (t <= 95.0833) return Mth.lerp(t - 94.0833F, -11.055F, -9.71F)
        if (t <= 96) return Mth.lerp((t - 95.0833F) / (96F - 95.0833F), -9.71F, -8.1F)
        if (t <= 96.9167) return Mth.lerp((t - 96F) / (96.9167F - 96F), -8.1F, -5.56F)
        if (t <= 97.75) return Mth.lerp((t - 96.9167F) / (97.75F - 96.9167F), -5.56F, -3.26F)
        if (t <= 99) return Mth.lerp((t - 97.75F) / (99F - 97.75F), -3.26F, -1.24F)

        return Mth.lerp(t - 99F, -1.24F, 0.1F)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 43) return Mth.lerp(t / 43F, -3F, 112.75F)
        if (t <= 44) return Mth.lerp(t - 43F, 112.75F, 114.53F)
        if (t <= 44.8333) return Mth.lerp((t - 44F) / (44.8333F - 44F), 114.53F, 116.005F)
        if (t <= 45.75) return Mth.lerp((t - 44.8333F) / (45.75F - 44.8333F), 116.005F, 116.49F)
        if (t <= 46.75) return Mth.lerp(t - 45.75F, 116.49F, 115.97F)
        if (t <= 48) return Mth.lerp((t - 46.75F) / (48F - 46.75F), 115.97F, 113.97F)
        if (t <= 49.25) return Mth.lerp((t - 48F) / (49.25F - 48F), 113.97F, 111.47F)
        if (t <= 54) return Mth.lerp((t - 49.25F) / (54F - 49.25F), 111.47F, 100.1F)
        if (t <= 55.1667) return Mth.lerp((t - 54F) / (55.1667F - 54F), 100.1F, 97.31F)
        if (t <= 56.6667) return Mth.lerp((t - 55.1667F) / (56.6667F - 55.1667F), 97.31F, 93.31F)
        if (t <= 86.6667) return Mth.lerp((t - 56.6667F) / (86.6667F - 56.6667F), 93.31F, 13.36F)
        if (t <= 87.1667) return Mth.lerp((t - 86.6667F) / (87.1667F - 86.6667F), 13.36F, 12.03F)
        if (t <= 87.75) return Mth.lerp((t - 87.1667F) / (87.75F - 87.1667F), 12.03F, 10.47F)
        if (t <= 88.3333) return Mth.lerp((t - 87.75F) / (88.3333F - 87.75F), 10.47F, 8.96F)
        if (t <= 88.8333) return Mth.lerp((t - 88.3333F) / (88.8333F - 88.3333F), 8.96F, 7.67F)
        if (t <= 94.0833) return Mth.lerp((t - 88.8333F) / (94.0833F - 88.8333F), 7.67F, -3.395F)
        if (t <= 95.0833) return Mth.lerp(t - 94.0833F, -3.395F, -5.05F)
        if (t <= 96) return Mth.lerp((t - 95.0833F) / (96F - 95.0833F), -5.05F, -6.17F)
        if (t <= 96.9167) return Mth.lerp((t - 96F) / (96.9167F - 96F), -6.17F, -6.64F)
        if (t <= 97.75) return Mth.lerp((t - 96.9167F) / (97.75F - 96.9167F), -6.64F, -6.25F)
        if (t <= 99) return Mth.lerp((t - 97.75F) / (99F - 97.75F), -6.25F, -4.99F)

        return Mth.lerp(t - 99F, -4.99F, -3F)
    }

    override fun getTrackDistance(): Float {
        return 2.25f
    }
}
