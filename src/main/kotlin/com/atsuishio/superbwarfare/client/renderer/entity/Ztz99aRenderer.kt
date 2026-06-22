package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Ztz99aRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun getBoneRotX(t: Float): Float {
        if (t <= 0.0833) return 0F
        if (t <= 0.75) return Mth.lerp((t - 0.0833F) / (0.75F - 0.0833F), 0F, -29.21F)
        if (t <= 2.8333) return -29.21F
        if (t <= 3.5833) return Mth.lerp((t - 2.8333F) / (3.5833F - 2.8333F), -29.21F, -59.21F)
        if (t <= 4.3333) return Mth.lerp((t - 3.5833F) / (4.3333F - 3.5833F), -59.21F, -83.86F)
        if (t <= 5.4167) return -83.86F
        if (t <= 6.25) return Mth.lerp((t - 5.4167F) / (6.25F - 5.4167F), -83.86F, -97.08F)
        if (t <= 7.1667) return Mth.lerp((t - 6.25F) / (7.1667F - 6.25F), -97.08F, -116.69F)
        if (t <= 8.3333) return -116.69F
        if (t <= 9.1667) return Mth.lerp((t - 8.3333F) / (9.1667F - 8.3333F), -116.69F, -130.97F)
        if (t <= 9.9167) return Mth.lerp((t - 9.1667F) / (9.9167F - 9.1667F), -130.97F, -147.88F)
        if (t <= 19.3333) return -147.88F
        if (t <= 20.0833) return Mth.lerp((t - 19.3333F) / (20.0833F - 19.3333F), -147.88F, -157.88F)
        if (t <= 20.8333) return Mth.lerp((t - 20.0833F) / (20.8333F - 20.0833F), -157.88F, -162F)
        if (t <= 22.9167) return -162F
        if (t <= 23.6667) return Mth.lerp((t - 22.9167F) / (23.6667F - 22.9167F), -162F, -168F)
        if (t <= 24.3333) return Mth.lerp((t - 23.6667F) / (24.3333F - 23.6667F), -168F, -180F)
        if (t <= 75.6667) return -180F
        if (t <= 76.25) return Mth.lerp((t - 75.6667F) / (76.25F - 75.6667F), -180F, -191F)
        if (t <= 76.8333) return Mth.lerp((t - 76.25F) / (76.8333F - 76.25F), -191F, -197F)
        if (t <= 79.3333) return -197F
        if (t <= 79.9167) return Mth.lerp((t - 79.3333F) / (79.9167F - 79.3333F), -197F, -207.79F)
        if (t <= 80.5833) return Mth.lerp((t - 79.9167F) / (80.5833F - 79.9167F), -207.79F, -210.79F)
        if (t <= 90.1667) return -210.79F
        if (t <= 90.8333) return Mth.lerp((t - 90.1667F) / (90.8333F - 90.1667F), -210.79F, -228.53F)
        if (t <= 91.5833) return Mth.lerp((t - 90.8333F) / (91.5833F - 90.8333F), -228.53F, -241.53F)
        if (t <= 92.8333) return -241.53F
        if (t <= 93.5833) return Mth.lerp((t - 92.8333F) / (93.5833F - 92.8333F), -241.53F, -260.47F)
        if (t <= 94.3333) return Mth.lerp((t - 93.5833F) / (94.3333F - 93.5833F), -260.47F, -277.47F)
        if (t <= 95.75) return -277.47F
        if (t <= 96.4167) return Mth.lerp((t - 95.75F) / (96.4167F - 95.75F), -277.47F, -301.92F)
        if (t <= 97.0833) return Mth.lerp((t - 96.4167F) / (97.0833F - 96.4167F), -301.92F, -325.58F)
        if (t <= 98.6667) return -325.58F
        if (t <= 99.3333) return Mth.lerp((t - 98.6667F) / (99.3333F - 98.6667F), -325.58F, -344.19F)

        return Mth.lerp((t - 99.3333F) / (100F - 99.3333F), -344.19F, -360F)
    }

    override fun getBoneMoveY(t: Float): Float {
        if (t <= 0.0833) return 0F
        if (t <= 0.75) return Mth.lerp((t - 0.0833F) / (0.75F - 0.0833F), 0F, -0.37F)
        if (t <= 2.8333) return Mth.lerp((t - 0.75F) / (2.8333F - 0.75F), -0.37F, -2.19F)
        if (t <= 3.5833) return Mth.lerp((t - 2.8333F) / (3.5833F - 2.8333F), -2.19F, -3.25F)
        if (t <= 4.4167) return Mth.lerp((t - 3.5833F) / (4.4167F - 3.5833F), -3.25F, -4.77F)
        if (t <= 5.4167) return Mth.lerp(t - 4.4167F, -4.77F, -6.59F)
        if (t <= 6.3333) return Mth.lerp((t - 5.4167F) / (6.3333F - 5.4167F), -6.59F, -8.25F)
        if (t <= 7.1667) return Mth.lerp((t - 6.3333F) / (7.1667F - 6.3333F), -8.25F, -9.66F)
        if (t <= 8.3333) return Mth.lerp((t - 7.1667F) / (8.3333F - 7.1667F), -9.66F, -11.34F)
        if (t <= 9.1667) return Mth.lerp((t - 8.3333F) / (9.1667F - 8.3333F), -11.34F, -12.75F)
        if (t <= 9.9167) return Mth.lerp((t - 9.1667F) / (9.9167F - 9.1667F), -12.75F, -13.73F)
        if (t <= 19.3333) return Mth.lerp((t - 9.9167F) / (19.3333F - 9.9167F), -13.73F, -22.14F)
        if (t <= 20.0833) return Mth.lerp((t - 19.3333F) / (20.0833F - 19.3333F), -22.14F, -22.65F)
        if (t <= 20.8333) return Mth.lerp((t - 20.0833F) / (20.8333F - 20.0833F), -22.65F, -23.25F)
        if (t <= 22.9167) return Mth.lerp((t - 20.8333F) / (22.9167F - 20.8333F), -23.25F, -24.54F)
        if (t <= 23.6667) return Mth.lerp((t - 22.9167F) / (23.6667F - 22.9167F), -24.54F, -24.7F)
        if (t <= 75.6667) return Mth.lerp((t - 23.6667F) / (75.6667F - 23.6667F), -24.7F, -24.8F)
        if (t <= 76.25) return Mth.lerp((t - 75.6667F) / (76.25F - 75.6667F), -24.8F, -24.65F)
        if (t <= 76.8333) return Mth.lerp((t - 76.25F) / (76.8333F - 76.25F), -24.65F, -24.47F)
        if (t <= 79.3333) return Mth.lerp((t - 76.8333F) / (79.3333F - 76.8333F), -24.47F, -23.18F)
        if (t <= 79.9167) return Mth.lerp((t - 79.3333F) / (79.9167F - 79.3333F), -23.18F, -22.7F)
        if (t <= 80.5833) return Mth.lerp((t - 79.9167F) / (80.5833F - 79.9167F), -22.7F, -22.19F)
        if (t <= 90.1667) return Mth.lerp((t - 80.5833F) / (90.1667F - 80.5833F), -22.19F, -13.51F)
        if (t <= 90.8333) return Mth.lerp((t - 90.1667F) / (90.8333F - 90.1667F), -13.51F, -12.75F)
        if (t <= 91.5833) return Mth.lerp((t - 90.8333F) / (91.5833F - 90.8333F), -12.75F, -11.69F)
        if (t <= 92.8333) return Mth.lerp((t - 91.5833F) / (92.8333F - 91.5833F), -11.69F, -9.57F)
        if (t <= 93.5833) return Mth.lerp((t - 92.8333F) / (93.5833F - 92.8333F), -9.57F, -8.3F)
        if (t <= 94.3333) return Mth.lerp((t - 93.5833F) / (94.3333F - 93.5833F), -8.3F, -7.03F)
        if (t <= 95.75) return Mth.lerp((t - 94.3333F) / (95.75F - 94.3333F), -7.03F, -4.53F)
        if (t <= 96.4167) return Mth.lerp((t - 95.75F) / (96.4167F - 95.75F), -4.53F, -3.5F)
        if (t <= 97.0833) return Mth.lerp((t - 96.4167F) / (97.0833F - 96.4167F), -3.5F, -2.64F)
        if (t <= 98.6667) return Mth.lerp((t - 97.0833F) / (98.6667F - 97.0833F), -2.64F, -0.75F)
        if (t <= 99.3333) return Mth.lerp((t - 98.6667F) / (99.3333F - 98.6667F), -0.75F, -0.3F)

        return Mth.lerp((t - 99.3333F) / (100F - 99.3333F), -0.3F, 0F)
    }

    override fun getBoneMoveZ(t: Float): Float {
        if (t <= 0.0833) return Mth.lerp(t / (0.0833F - 0F), 0F, 125F)
        if (t <= 0.75) return Mth.lerp((t - 0.0833F) / (0.75F - 0.0833F), 125F, 125.715F)
        if (t <= 2.8333) return Mth.lerp((t - 0.75F) / (2.8333F - 0.75F), 125.715F, 128.545F)
        if (t <= 3.5833) return Mth.lerp((t - 2.8333F) / (3.5833F - 2.8333F), 128.545F, 129.565F)
        if (t <= 4.4167) return Mth.lerp((t - 3.5833F) / (4.4167F - 3.5833F), 129.565F, 130.245F)
        if (t <= 5.4167) return Mth.lerp(t - 4.4167F, 130.245F, 130.455F)
        if (t <= 6.3333) return Mth.lerp((t - 5.4167F) / (6.3333F - 5.4167F), 130.455F, 130.365F)
        if (t <= 7.1667) return Mth.lerp((t - 6.3333F) / (7.1667F - 6.3333F), 130.365F, 129.875F)
        if (t <= 8.3333) return Mth.lerp((t - 7.1667F) / (8.3333F - 7.1667F), 129.875F, 129.145F)
        if (t <= 9.1667) return Mth.lerp((t - 8.3333F) / (9.1667F - 8.3333F), 129.145F, 128.065F)
        if (t <= 9.9167) return Mth.lerp((t - 9.1667F) / (9.9167F - 9.1667F), 128.065F, 126.985F)
        if (t <= 19.3333) return Mth.lerp((t - 9.9167F) / (19.3333F - 9.9167F), 126.985F, 113.445F)
        if (t <= 20.0833) return Mth.lerp((t - 19.3333F) / (20.0833F - 19.3333F), 113.445F, 112.365F)
        if (t <= 20.8333) return Mth.lerp((t - 20.0833F) / (20.8333F - 20.0833F), 112.365F, 111.085F)
        if (t <= 22.9167) return Mth.lerp((t - 20.8333F) / (22.9167F - 20.8333F), 111.085F, 107.545F)
        if (t <= 23.6667) return Mth.lerp((t - 22.9167F) / (23.6667F - 22.9167F), 107.545F, 106.265F)
        if (t <= 75.6667) return Mth.lerp((t - 23.6667F) / (75.6667F - 23.6667F), 106.265F, 17.69F)
        if (t <= 76.25) return Mth.lerp((t - 75.6667F) / (76.25F - 75.6667F), 17.69F, 16.7F)
        if (t <= 76.8333) return Mth.lerp((t - 76.25F) / (76.8333F - 76.25F), 16.7F, 15.78F)
        if (t <= 79.3333) return Mth.lerp((t - 76.8333F) / (79.3333F - 76.8333F), 15.78F, 11.82F)
        if (t <= 79.9167) return Mth.lerp((t - 79.3333F) / (79.9167F - 79.3333F), 11.82F, 10.9F)
        if (t <= 80.5833) return Mth.lerp((t - 79.9167F) / (80.5833F - 79.9167F), 10.9F, 9.88F)
        if (t <= 90.1667) return Mth.lerp((t - 80.5833F) / (90.1667F - 80.5833F), 9.88F, -3.95F)
        if (t <= 90.8333) return Mth.lerp((t - 90.1667F) / (90.8333F - 90.1667F), -3.95F, -4.76F)
        if (t <= 91.5833) return Mth.lerp((t - 90.8333F) / (91.5833F - 90.8333F), -4.76F, -5.53F)
        if (t <= 92.8333) return Mth.lerp((t - 91.5833F) / (92.8333F - 91.5833F), -5.53F, -6.61F)
        if (t <= 93.5833) return Mth.lerp((t - 92.8333F) / (93.5833F - 92.8333F), -6.61F, -6.86F)
        if (t <= 94.3333) return -6.86F
        if (t <= 95.75) return Mth.lerp((t - 94.3333F) / (95.75F - 94.3333F), -6.86F, -6.49F)
        if (t <= 96.4167) return Mth.lerp((t - 95.75F) / (96.4167F - 95.75F), -6.49F, -6.01F)
        if (t <= 97.0833) return Mth.lerp((t - 96.4167F) / (97.0833F - 96.4167F), -6.01F, -5.1F)
        if (t <= 98.6667) return Mth.lerp((t - 97.0833F) / (98.6667F - 97.0833F), -5.1F, -2.28F)
        if (t <= 99.3333) return Mth.lerp((t - 98.6667F) / (99.3333F - 98.6667F), -2.28F, -1.02F)

        return Mth.lerp((t - 99.3333F) / (100F - 99.3333F), -1.02F, 0F)
    }

    override fun getTrackDistance(): Float {
        return 2.7f
    }
}
