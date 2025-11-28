package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;


public class Hpj11Model extends VehicleModel<Hpj11Entity> {

    @Override
    public @Nullable TransformContext<Hpj11Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "radar2" -> (bone, vehicle, state) -> {
                Player player = Minecraft.getInstance().player;
                bone.setHidden(vehicle.getNthEntity(vehicle.getTurretControllerIndex()) == player && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle));
            };

            case "rdr", "rdr2" ->
                    (bone, vehicle, state) -> bone.setRotX(getAnimationProcessor().getBone("barrel").getRotX());

            case "paoguanroll" -> (bone, vehicle, state) -> {
                var gunData = vehicle.getGunData(0);

                if (gunData != null) {
                    bone.setRotZ(bone.getRotZ() + gunData.shootTimer.get());
                }
            };

            case "flare" -> (bone, vehicle, state) -> {
                var gunData = vehicle.getGunData(0);

                if (gunData != null) {
                    bone.setHidden(gunData.shootTimer.get() <= 2);
                } else {
                    bone.setHidden(true);
                }

                bone.setScaleX((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setScaleY((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            };

            default -> super.collectTransform(boneName);
        };

    }

//    @Override
//    public boolean hideForTurretControllerWhileZooming() {
//        return true;
//    }
}
