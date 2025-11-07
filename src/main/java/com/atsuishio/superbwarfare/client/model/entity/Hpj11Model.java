package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity.ANIM_TIME;

public class Hpj11Model extends VehicleModel<Hpj11Entity> {

    @Override
    public @Nullable TransformContext<Hpj11Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "radar2" -> (bone, vehicle, state) -> {
                Player player = Minecraft.getInstance().player;
                bone.setHidden(vehicle.getFirstPassenger() == player && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
            };

            case "barrel", "rdr", "rdr2" ->
                    (bone, vehicle, state) -> bone.setRotX(-Mth.lerp(state.getPartialTick(), vehicle.xRotO, vehicle.getXRot()) * Mth.DEG_TO_RAD);

            case "paoguanroll" -> (bone, vehicle, state) ->
                    bone.setRotZ(-Mth.lerp(state.getPartialTick(), vehicle.gunRotO, vehicle.getGunRot()));

            case "flare" -> (bone, vehicle, state) -> {
                bone.setHidden(vehicle.getEntityData().get(ANIM_TIME) == 0);
                bone.setScaleX((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setScaleY((float) (2 + 0.8 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            };

            default -> super.collectTransform(boneName);
        };

    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
