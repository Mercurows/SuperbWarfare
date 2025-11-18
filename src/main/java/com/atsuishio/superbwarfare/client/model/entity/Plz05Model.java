package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Plz05Entity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class Plz05Model extends VehicleModel<Plz05Entity> {

    @Override
    public @Nullable TransformContext<Plz05Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "titop1" -> (bone, vehicle, state) -> {
                Player player = Minecraft.getInstance().player;
                bone.setHidden(vehicle.getNthEntity(vehicle.getTurretControllerIndex()) == player && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
            };
            default -> super.collectTransform(boneName);
        };

    }

    @Override
    public float getBoneRotX(float t) {
        if (t <= 43.6667) return 0F;
        if (t <= 44.3333) return Mth.lerp((t - 43.6667F) / (44.3333F - 43.6667F), 0F, -45F);
        if (t <= 45.6667) return Mth.lerp((t - 44.3333F) / (45.6667F - 44.3333F), -45F, -67.5F);
        if (t <= 46.3333) return Mth.lerp((t - 45.6667F) / (46.3333F - 45.6667F), -67.5F, -90F);
        if (t <= 47.6667) return -90F;
        if (t <= 48.3333) return Mth.lerp((t - 47.6667F) / (48.3333F - 47.6667F), -90F, -135F);
        if (t <= 49.3333) return Mth.lerp(t - 48.3333F, -135F, -145F);
        if (t <= 50) return Mth.lerp((t - 49.3333F) / (50F - 49.3333F), -145F, -154.5F);
        if (t <= 54.6667) return -154.5F;
        if (t <= 55.3333) return Mth.lerp((t - 54.6667F) / (55.3333F - 54.6667F), -154.5F, -180F);
        if (t <= 86.4167) return -180F;
        if (t <= 87.0833) return Mth.lerp((t - 86.4167F) / (87.0833F - 86.4167F), -180F, -200F);
        if (t <= 95) return Mth.lerp((t - 87.0833F) / (95F - 87.0833F), -200F, -210F);
        if (t <= 95.6667) return Mth.lerp((t - 95F) / (95.6667F - 95F), -210F, -225F);
        if (t <= 96.3333) return Mth.lerp((t - 95.6667F) / (96.3333F - 95.6667F), -225F, -247.5F);
        if (t <= 97) return Mth.lerp((t - 96.3333F) / (97F - 96.3333F), -247.5F, -270F);
        if (t <= 98) return Mth.lerp(t - 97F, -270F, -272.5F);
        if (t <= 98.6667) return Mth.lerp((t - 98F) / (98.6667F - 98F), -272.5F, -315F);
        if (t <= 99.6667) return Mth.lerp(t - 98.6667F, -315F, -337.5F);
        if (t <= 99.9999) return Mth.lerp((t - 99.6667F) / (100F - 99.6667F), -337.5F, -360F);

        return 0F;
    }

    @Override
    public float getBoneMoveY(float t) {
        if (t <= 44) return Mth.lerp(t / 44F, 0F, -0.6F);
        if (t <= 46) return Mth.lerp((t - 44F) / (46F - 44F), -0.6F, -4.13F);
        if (t <= 48) return Mth.lerp((t - 46F) / (48F - 46F), -4.13F, -9.565F);
        if (t <= 49.5833) return Mth.lerp((t - 48F) / (49.5833F - 48F), -9.565F, -12.32F);
        if (t <= 55) return Mth.lerp((t - 49.5833F) / (55F - 49.5833F), -12.32F, -19.71F);
        if (t <= 86.75) return Mth.lerp((t - 55F) / (86.75F - 55F), -19.71F, -19.67F);
        if (t <= 95.3333) return Mth.lerp((t - 86.75F) / (95.3333F - 86.75F), -19.67F, -11.005F);
        if (t <= 96.6667) return Mth.lerp((t - 95.3333F) / (96.6667F - 95.3333F), -11.005F, -8.35F);
        if (t <= 98.3333) return Mth.lerp((t - 96.6667F) / (98.3333F - 96.6667F), -8.35F, -3.285F);

        return Mth.lerp((t - 98.3333F) / (100F - 98.3333F), -3.285F, 0F);
    }

    @Override
    public float getBoneMoveZ(float t) {
        if (t <= 44) return Mth.lerp(t / 44F, 0F, 133.75F);
        if (t <= 46) return Mth.lerp((t - 44F) / (46F - 44F), 133.75F, 137.405F);
        if (t <= 48) return Mth.lerp((t - 46F) / (48F - 46F), 137.405F, 137.47F);
        if (t <= 49.5833) return Mth.lerp((t - 48F) / (49.5833F - 48F), 137.47F, 134.72F);
        if (t <= 55) return Mth.lerp((t - 49.5833F) / (55F - 49.5833F), 134.72F, 119.36F);
        if (t <= 86.75) return Mth.lerp((t - 55F) / (86.75F - 55F), 119.36F, 23.32F);
        if (t <= 95.3333) return Mth.lerp((t - 86.75F) / (95.3333F - 86.75F), 23.32F, -0.695F);
        if (t <= 96.6667) return Mth.lerp((t - 95.3333F) / (96.6667F - 95.3333F), -0.695F, -3.205F);
        if (t <= 98.3333) return Mth.lerp((t - 96.6667F) / (98.3333F - 96.6667F), -3.205F, -3.28F);

        return Mth.lerp((t - 98.3333F) / (100F - 98.3333F), -3.28F, 0F);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
