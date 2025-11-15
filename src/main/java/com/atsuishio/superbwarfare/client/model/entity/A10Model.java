package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class A10Model extends VehicleModel<A10Entity> {

    @Override
    public @Nullable TransformContext<A10Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "root" -> (bone, vehicle, state) ->
                    bone.setHidden(hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 2);

            case "wingLR" -> (bone, vehicle, state) ->
                    bone.setRotX(1.5f * Mth.lerp(state.getPartialTick(), vehicle.flap1LRotO, vehicle.getFlap1LRot()) * Mth.DEG_TO_RAD);

            case "wingRR" -> (bone, vehicle, state) ->
                    bone.setRotX(1.5f * Mth.lerp(state.getPartialTick(), vehicle.flap1RRotO, vehicle.getFlap1RRot()) * Mth.DEG_TO_RAD);

            case "wingLR2" -> (bone, vehicle, state) ->
                    bone.setRotX(1.5f * Mth.lerp(state.getPartialTick(), vehicle.flap1L2RotO, vehicle.getFlap1L2Rot()) * Mth.DEG_TO_RAD);

            case "wingRR2" -> (bone, vehicle, state) ->
                    bone.setRotX(1.5f * Mth.lerp(state.getPartialTick(), vehicle.flap1R2RotO, vehicle.getFlap1R2Rot()) * Mth.DEG_TO_RAD);

            case "wingLB" -> (bone, vehicle, state) ->
                    bone.setRotX(Mth.lerp(state.getPartialTick(), vehicle.flap2LRotO, vehicle.getFlap2LRot()) * Mth.DEG_TO_RAD);

            case "wingRB" -> (bone, vehicle, state) ->
                    bone.setRotX(Mth.lerp(state.getPartialTick(), vehicle.flap2RRotO, vehicle.getFlap2RRot()) * Mth.DEG_TO_RAD);

            case "weiyiL", "weiyiR" -> (bone, vehicle, state) ->
                    bone.setRotY(Mth.clamp(Mth.lerp(state.getPartialTick(), vehicle.flap3RotO, vehicle.getFlap3Rot()), -20f, 20f) * Mth.DEG_TO_RAD);

            case "gear", "gear2", "gear3" ->
                    (bone, vehicle, state) -> bone.setRotX(vehicle.gearRot(state.getPartialTick()) * Mth.DEG_TO_RAD);

            case "qianzhou", "qianzhou2" ->
                    (bone, vehicle, state) -> bone.setRotZ(Mth.lerp(state.getPartialTick(), vehicle.propellerRotO, vehicle.getPropellerRot()));

            case "bomb1" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideBomb(vehicle, 3));

            case "bomb2" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideBomb(vehicle, 2));

            case "bomb3" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideBomb(vehicle, 1));

            case "missile1" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideMissile(vehicle, 4));

            case "missile2" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideMissile(vehicle, 3));

            case "missile4" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideMissile(vehicle, 2));

            case "missile3" -> (bone, vehicle, state) ->
                    bone.setHidden(shouldHideMissile(vehicle, 1));

            default -> null;
        };
    }

    public boolean shouldHideBomb(VehicleEntity vehicle, int ammo) {
        var gunData = vehicle.getGunData("Bomb");
        if (gunData == null) {
            return false;
        } else {
            return gunData.ammo.get() < ammo;
        }
    }

    public boolean shouldHideMissile(VehicleEntity vehicle, int ammo) {
        var gunData = vehicle.getGunData("Missile");
        if (gunData == null) {
            return false;
        } else {
            return gunData.ammo.get() < ammo;
        }
    }
}

