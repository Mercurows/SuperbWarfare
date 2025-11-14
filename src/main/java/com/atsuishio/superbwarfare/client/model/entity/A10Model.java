package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.GEAR_ROT;

public class A10Model extends VehicleModel<A10Entity> {

    @Override
    public @Nullable TransformContext<A10Entity> collectTransform(String boneName) {
        return switch (boneName) {
            case "root" -> (bone, vehicle, state) ->
                    bone.setHidden(hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 2);

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
                    (bone, vehicle, state) -> bone.setRotX(Mth.lerp(state.getPartialTick(), vehicle.gearRotO, vehicle.getEntityData().get(GEAR_ROT)) * Mth.DEG_TO_RAD);

            case "qianzhou", "qianzhou2" ->
                    (bone, vehicle, state) -> bone.setRotZ(Mth.lerp(state.getPartialTick(), vehicle.propellerRotO, vehicle.getPropellerRot()));

//            case "bomb1" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_BOMB) < 3);
//
//            case "bomb2" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_BOMB) < 2);
//
//            case "bomb3" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_BOMB) < 1);
//
//            case "missile1" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_MISSILE) < 4);
//
//            case "missile2" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_MISSILE) < 3);
//
//            case "missile4" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_MISSILE) < 2);
//
//            case "missile3" -> (bone, vehicle, state) ->
//                    bone.setHidden(vehicle.getEntityData().get(LOADED_MISSILE) < 1);

            default -> null;
        };
    }
}
