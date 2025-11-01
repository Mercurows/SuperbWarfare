package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class Bmp2Model extends VehicleModel<Bmp2Entity> {

    @Override
    public @Nullable TransformContext<Bmp2Entity> collectTransform(String boneName) {
        // TODO 车身晃动干哪去了
        if (boneName.equals("base")) {
            Player player = Minecraft.getInstance().player;
            return (bone, vehicle, state) -> {
                bone.setHidden(player != null && vehicle == player.getVehicle() && vehicle.getFirstPassenger() != player && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle));
                super.collectTransform(boneName);
            };
        }
        return super.collectTransform(boneName);
    }

    @Override
    public float getBoneRotX(float t) {
        if (t <= 37.6667) return 0F;
        if (t <= 38.5833) return Mth.lerp((t - 37.6667F) / (38.5833F - 37.6667F), 0F, -45F);
        if (t <= 39.75) return -45F;
        if (t <= 40.6667) return Mth.lerp((t - 39.75F) / (40.6667F - 39.75F), -45F, -90F);
        if (t <= 41.6667) return -90F;
        if (t <= 42.5) return -90F;
        if (t <= 43.5) return Mth.lerp(t - 42.5F, -90F, -135F);
        if (t <= 44.5833) return -135F;
        if (t <= 45.0833) return Mth.lerp((t - 44.5833F) / (45.0833F - 44.5833F), -135F, -150F);
        if (t <= 52.25) return -150F;
        if (t <= 52.75) return Mth.lerp((t - 52.25F) / (52.75F - 52.25F), -150F, -180F);
        if (t <= 84.3333) return -180F;
        if (t <= 84.9167) return Mth.lerp((t - 84.3333F) / (84.9167F - 84.3333F), -180F, -210F);
        if (t <= 92.5833) return -210F;
        if (t <= 93.4167) return Mth.lerp((t - 92.5833F) / (93.4167F - 92.5833F), -210F, -220F);
        if (t <= 94.25) return -220F;
        if (t <= 94.9167) return Mth.lerp((t - 94.25F) / (94.9167F - 94.25F), -220F, -243.33F);
        if (t <= 95.75) return Mth.lerp((t - 94.9167F) / (95.75F - 94.9167F), -243.33F, -270F);
        if (t <= 96.8333) return -270F;
        if (t <= 97.5833) return Mth.lerp((t - 96.8333F) / (97.5833F - 96.8333F), -270F, -315F);
        if (t <= 98.8333) return -315F;
        if (t <= 99.5833) return Mth.lerp((t - 98.8333F) / (99.5833F - 98.8333F), -315F, -360F);

        return 0F;
    }

    @Override
    public float getBoneMoveY(float t) {
        if (t <= 37.6667) return 0F;
        if (t <= 38.5833) return Mth.lerp((t - 37.6667F) / (38.5833F - 37.6667F), 0F, -1.8F);
        if (t <= 40.3333) return Mth.lerp((t - 38.5833F) / (40.3333F - 38.5833F), -1.8F, -4.1F);
        if (t <= 42.9167) return Mth.lerp((t - 40.3333F) / (42.9167F - 40.3333F), -4.1F, -10.3F);
        if (t <= 44.25) return Mth.lerp((t - 42.9167F) / (44.25F - 42.9167F), -10.3F, -12.9F);
        if (t <= 52.4167) return Mth.lerp((t - 44.25F) / (52.4167F - 44.25F), -12.9F, -23.96F);
        if (t <= 84.5833) return -23.96F;
        if (t <= 93) return Mth.lerp((t - 84.5833F) / (93F - 84.5833F), -23.96F, -12.93F);
        if (t <= 95.25) return Mth.lerp((t - 93F) / (95.25F - 93F), -12.93F, -10.085F);
        if (t <= 97.5) return Mth.lerp((t - 95.25F) / (97.5F - 95.25F), -10.085F, -4.585F);
        if (t <= 98.8333) return Mth.lerp((t - 97.5F) / (98.8333F - 97.5F), -4.585F, -1.165F);
        if (t <= 99.25) return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -1.165F, -0.25F);

        return Mth.lerp((t - 99.25F) / (100F - 99.25F), -0.25F, 0F);
    }

    @Override
    public float getBoneMoveZ(float t) {
        if (t <= 37.6667) return Mth.lerp(t / (37.6667F - 0F), 0F, 111.6F);
        if (t <= 38.5833) return Mth.lerp((t - 37.6667F) / (38.5833F - 37.6667F), 111.6F, 113.25F);
        if (t <= 40.3333) return Mth.lerp((t - 38.5833F) / (40.3333F - 38.5833F), 113.25F, 116F);
        if (t <= 42.9167) return 116F;
        if (t <= 44.25) return Mth.lerp((t - 42.9167F) / (44.25F - 42.9167F), 116F, 113.5F);
        if (t <= 52.4167) return Mth.lerp((t - 44.25F) / (52.4167F - 44.25F), 113.5F, 96.25F);
        if (t <= 84.5833) return Mth.lerp((t - 52.4167F) / (84.5833F - 52.4167F), 96.25F, 14.095F);
        if (t <= 93) return Mth.lerp((t - 84.5833F) / (93F - 84.5833F), 14.095F, -3.565F);
        if (t <= 95.25) return Mth.lerp((t - 93F) / (95.25F - 93F), -3.565F, -6.35F);
        if (t <= 97.5) return Mth.lerp((t - 95.25F) / (97.5F - 95.25F), -6.35F, -6.39F);
        if (t <= 98.8333) return Mth.lerp((t - 97.5F) / (98.8333F - 97.5F), -6.39F, -3.03F);
        if (t <= 99.25) return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -3.03F, -1.95F);

        return Mth.lerp((t - 99.25F) / (100F - 99.25F), -1.95F, 0F);
    }

    @Override
    public boolean hideFor1stPassengerWhileZooming() {
        return true;
    }
}
