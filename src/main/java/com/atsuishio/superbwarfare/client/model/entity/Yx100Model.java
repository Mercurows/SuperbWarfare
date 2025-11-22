package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import net.minecraft.util.Mth;

public class Yx100Model extends VehicleModel<Yx100Entity> {

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public float getBoneRotX(float t) {
        if (t <= 34.75) return 0F;
        if (t <= 35.5) return Mth.lerp((t - 34.75F) / (35.5F - 34.75F), 0F, -45F);
        if (t <= 35.8333) return -45F;
        if (t <= 36.5) return Mth.lerp((t - 35.8333F) / (36.5F - 35.8333F), -45F, -90F);
        if (t <= 36.6667) return -90F;
        if (t <= 37) return Mth.lerp((t - 36.6667F) / (37F - 36.6667F), -90F, -112.5F);
        if (t <= 37.3333) return -112.5F;
        if (t <= 37.5) return -112.5F;
        if (t <= 38.1667) return Mth.lerp((t - 37.5F) / (38.1667F - 37.5F), -112.5F, -135F);
        if (t <= 41.9167) return -135F;
        if (t <= 42.4167) return Mth.lerp((t - 41.9167F) / (42.4167F - 41.9167F), -135F, -157.5F);
        if (t <= 43.1667) return -157.5F;
        if (t <= 43.6667) return Mth.lerp((t - 43.1667F) / (43.6667F - 43.1667F), -157.5F, -180F);
        if (t <= 68) return -180F;
        if (t <= 68.5) return Mth.lerp((t - 68F) / (68.5F - 68F), -180F, -202.5F);
        if (t <= 69.25) return -202.5F;
        if (t <= 69.8333) return Mth.lerp((t - 69.25F) / (69.8333F - 69.25F), -202.5F, -220F);
        if (t <= 73.5) return -220F;
        if (t <= 74.1667) return Mth.lerp((t - 73.5F) / (74.1667F - 73.5F), -220F, -242.5F);
        if (t <= 75.6667) return -242.5F;
        if (t <= 76.1667) return Mth.lerp((t - 75.6667F) / (76.1667F - 75.6667F), -242.5F, -295F);
        if (t <= 76.6667) return -295F;
        if (t <= 77.1667) return Mth.lerp((t - 76.6667F) / (77.1667F - 76.6667F), -295F, -340F);
        if (t <= 77.8333) return Mth.lerp((t - 77.1667F) / (77.8333F - 77.1667F), -340F, -360F);
        if (t <= 79.5) return -360F;

        return 0F;
    }

    @Override
    public float getBoneMoveY(float t) {
        if (t <= 35.1667) return 0F;
        if (t <= 36.1667) return Mth.lerp(t - 35.1667F, 0F, -2.91F);
        if (t <= 37) return Mth.lerp((t - 36.1667F) / (37F - 36.1667F), -2.91F, -6.79F);
        if (t <= 37.8333) return Mth.lerp((t - 37F) / (37.8333F - 37F), -6.79F, -10.005F);
        if (t <= 42.1667) return Mth.lerp((t - 37.8333F) / (42.1667F - 37.8333F), -10.005F, -22.38F);
        if (t <= 43.4167) return Mth.lerp((t - 42.1667F) / (43.4167F - 42.1667F), -22.38F, -24.14F);
        if (t <= 68.25) return -24.14F;
        if (t <= 69.5) return Mth.lerp((t - 68.25F) / (69.5F - 68.25F), -24.14F, -22.45F);
        if (t <= 73.8333) return Mth.lerp((t - 69.5F) / (73.8333F - 69.5F), -22.45F, -11.12F);
        if (t <= 75.9167) return Mth.lerp((t - 73.8333F) / (75.9167F - 73.8333F), -11.12F, -4.155F);
        if (t <= 76.9167) return Mth.lerp(t - 75.9167F, -4.155F, -0.855F);
        if (t <= 78.0833) return Mth.lerp((t - 76.9167F) / (78.0833F - 76.9167F), -0.855F, 0F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -0.025F, 0F);
    }

    @Override
    public float getBoneMoveZ(float t) {
        if (t <= 35.1667) return Mth.lerp(t / (35.1667F - 0F), 0F, 121.385F);
        if (t <= 36.1667) return Mth.lerp(t - 35.1667F, 121.385F, 124.37F);
        if (t <= 37) return 124.37F;
        if (t <= 37.8333) return Mth.lerp((t - 37F) / (37.8333F - 37F), 124.37F, 122.73F);
        if (t <= 42.1667) return Mth.lerp((t - 37.8333F) / (42.1667F - 37.8333F), 122.73F, 110.455F);
        if (t <= 43.4167) return Mth.lerp((t - 42.1667F) / (43.4167F - 42.1667F), 110.455F, 105.805F);
        if (t <= 68.25) return Mth.lerp((t - 43.4167F) / (68.25F - 43.4167F), 105.805F, 10.09F);
        if (t <= 69.5) return Mth.lerp((t - 68.25F) / (69.5F - 68.25F), 10.09F, 5.625F);
        if (t <= 73.8333) return Mth.lerp((t - 69.5F) / (73.8333F - 69.5F), 5.625F, -8.025F);
        if (t <= 75.9167) return Mth.lerp((t - 73.8333F) / (75.9167F - 73.8333F), -8.025F, -11.175F);
        if (t <= 76.9167) return Mth.lerp(t - 75.9167F, -11.175F, -9.35F);
        if (t <= 78.0833) return Mth.lerp((t - 76.9167F) / (78.0833F - 76.9167F), -9.35F, -5.38F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -4.12F, 0F);
    }
}
