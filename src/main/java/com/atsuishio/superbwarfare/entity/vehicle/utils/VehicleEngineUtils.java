package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

// TODO 等一个优化方案
public final class VehicleEngineUtils {

    public static void trackEngine(VehicleEntity vehicle, EngineInfo.Track engineInfo) {
        double buoyancy = engineInfo.buoyancy;
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));
        double wheelRotSpeed = engineInfo.wheelRotSpeed;
        double wheelDifferential = engineInfo.wheelDifferential;
        double trackSpeed = engineInfo.trackRotSpeed;
        double trackDifferential = engineInfo.trackDifferential;
        float maxForwardSpeedRate = engineInfo.maxForwardSpeedRate;
        float maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate;
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float steeringSpeed = engineInfo.steeringSpeed;

        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, fluidFloat, 0));
        }

        if (vehicle.onGround()) {
            float f0 = 0.54f + 0.25f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.05 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f0, 0.99, f0));
        } else if (vehicle.isInWater()) {
            float f1 = 0.74f + 0.09f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.04 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f1, 0.85, f1));
        } else {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        Entity passenger0 = vehicle.getFirstPassenger();

        if (vehicle.getEnergy() <= energyCost) return;

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false);
            vehicle.setRightInputDown(false);
            vehicle.setForwardInputDown(false);
            vehicle.setBackInputDown(false);
            vehicle.getEntityData().set(POWER, 0f);
        }

        if (vehicle.forwardInputDown()) {
            vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + (vehicle.getEntityData().get(POWER) < 0 ? powerAdd * 2f : powerAdd), 1));
        }

        if (vehicle.backInputDown()) {
            vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - (vehicle.getEntityData().get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
            if (vehicle.rightInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + steeringSpeed);
            } else if (vehicle.leftInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - steeringSpeed);
            }
        } else {
            if (vehicle.rightInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - steeringSpeed);
            } else if (vehicle.leftInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + steeringSpeed);
            }
        }

        if (vehicle.getEntityData().get(POWER) > 0) {
            vehicle.targetSpeed = maxForwardSpeedRate * (1 + vehicle.getXRot() / 55);
        } else {
            vehicle.targetSpeed = maxBackwardSpeedRate * (1 - vehicle.getXRot() / 55);
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        if (vehicle.upInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.6f);
        }

        if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        if (vehicle.level() instanceof ServerLevel) {
            vehicle.consumeEnergy(energyCost);
        }

        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * (float) Math.max(0.76f - 0.1f * vehicle.getDeltaMovement().horizontalDistance(), 0.3));

        double s0 = vehicle.getDeltaMovement().dot(vehicle.getViewVector(1));

        vehicle.setLeftWheelRot((float) ((vehicle.getLeftWheelRot() - wheelRotSpeed * s0) + Mth.clamp(wheelDifferential * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f)));
        vehicle.setRightWheelRot((float) ((vehicle.getRightWheelRot() - wheelRotSpeed * s0) - Mth.clamp(wheelDifferential * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f)));

        vehicle.setLeftTrack((float) ((vehicle.getLeftTrack() - trackSpeed * Math.PI * s0) + Mth.clamp(trackDifferential * Math.PI * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f)));
        vehicle.setRightTrack((float) ((vehicle.getRightTrack() - trackSpeed * Math.PI * s0) - Mth.clamp(trackDifferential * Math.PI * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f)));

        int i;
        if (vehicle.getEntityData().get(L_WHEEL_DAMAGED) && vehicle.getEntityData().get(R_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.93f);
            i = 0;
        } else if (vehicle.getEntityData().get(L_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.975f);
            i = 3;
        } else if (vehicle.getEntityData().get(R_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.975f);
            i = -3;
        } else {
            i = 0;
        }

        if (vehicle.getEntityData().get(MAIN_ENGINE_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        vehicle.setYRot((float) (vehicle.getYRot() - (vehicle.isInWater() && !vehicle.onGround() ? 2.5 : 6) * vehicle.getEntityData().get(DELTA_ROT) - i * s0));
        if (vehicle.isInWater() || vehicle.onGround()) {
            double water = (!vehicle.isInWater() && !vehicle.onGround() ? 0.05f : (vehicle.isInWater() && !vehicle.onGround() ? 0.3f : 1));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.15 * water * vehicle.targetSpeed * vehicle.getEntityData().get(POWER))));
        }
    }

    public static void wheelEngine(VehicleEntity vehicle, EngineInfo.Wheel engineInfo) {
        double buoyancy = engineInfo.buoyancy;
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));
        double wheelRotSpeed = engineInfo.wheelRotSpeed;
        double wheelDifferential = engineInfo.wheelDifferential;
        float maxForwardSpeedRate = engineInfo.maxForwardSpeedRate;
        float maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate;
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float steeringSpeed = engineInfo.steeringSpeed;

        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, fluidFloat, 0));
        }

        if (vehicle.onGround()) {
            float f0 = 0.54f + 0.25f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.05 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f0, 0.99, f0));
        } else if (vehicle.isInWater()) {
            float f1 = 0.74f + 0.09f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.04 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f1, 0.85, f1));
        } else {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (vehicle.level() instanceof ServerLevel serverLevel && vehicle.isInWater() && vehicle.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, vehicle.getX() + 0.5 * vehicle.getDeltaMovement().x, vehicle.getY() + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2, vehicle.getZ() + 0.5 * vehicle.getDeltaMovement().z, (int) (2 + 4 * vehicle.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, vehicle.getX() + 0.5 * vehicle.getDeltaMovement().x, vehicle.getY() + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2, vehicle.getZ() + 0.5 * vehicle.getDeltaMovement().z, (int) (2 + 10 * vehicle.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
        }

        Entity passenger0 = vehicle.getFirstPassenger();

        if (vehicle.getEnergy() <= energyCost) return;

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false);
            vehicle.setRightInputDown(false);
            vehicle.setForwardInputDown(false);
            vehicle.setBackInputDown(false);
            vehicle.getEntityData().set(POWER, 0f);
        }

        if (vehicle.forwardInputDown()) {
            vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + (vehicle.getEntityData().get(POWER) < 0 ? powerAdd * 2f : powerAdd), 1));
        }

        if (vehicle.backInputDown()) {
            vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - (vehicle.getEntityData().get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
        }

        if (vehicle.getEntityData().get(POWER) > 0) {
            vehicle.targetSpeed = maxForwardSpeedRate * (1 + vehicle.getXRot() / 55);
        } else {
            vehicle.targetSpeed = maxBackwardSpeedRate * (1 - vehicle.getXRot() / 55);
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.97f);
        }

        if (vehicle.upInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.6f);
        }

        if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.98f);
        }

        if (vehicle.level() instanceof ServerLevel) {
            vehicle.consumeEnergy(energyCost);
        }

        int i;
        if (vehicle.getEntityData().get(L_WHEEL_DAMAGED) && vehicle.getEntityData().get(R_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.93f);
            i = 0;
        } else if (vehicle.getEntityData().get(L_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.975f);
            i = 3;
        } else if (vehicle.getEntityData().get(R_WHEEL_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.975f);
            i = -3;
        } else {
            i = 0;
        }

        if (vehicle.getEntityData().get(MAIN_ENGINE_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.875f);
        }

        if (vehicle.rightInputDown()) {
            vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + steeringSpeed);
        } else if (vehicle.leftInputDown()) {
            vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - steeringSpeed);
        }

        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * (float) Math.max(0.78f - 0.25f * vehicle.getDeltaMovement().horizontalDistance(), 0.1));

        double s0 = vehicle.getDeltaMovement().dot(vehicle.getViewVector(1));

        vehicle.setLeftWheelRot((float) ((vehicle.getLeftWheelRot() - wheelRotSpeed * s0) - Mth.clamp(wheelDifferential * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f) * vehicle.getDeltaMovement().length()));
        vehicle.setRightWheelRot((float) ((vehicle.getRightWheelRot() - wheelRotSpeed * s0) + Mth.clamp(wheelDifferential * vehicle.getEntityData().get(DELTA_ROT), -5f, 5f) * vehicle.getDeltaMovement().length()));

        vehicle.setRudderRot(Mth.clamp(vehicle.getRudderRot() - vehicle.getEntityData().get(DELTA_ROT), -0.8f, 0.8f) * 0.75f);

        vehicle.setYRot((float) (vehicle.getYRot() - Math.max((vehicle.isInWater() && !vehicle.onGround() ? 6 : 12) * vehicle.getDeltaMovement().horizontalDistance(), 0) * vehicle.getRudderRot() * (vehicle.getEntityData().get(POWER) > 0 ? 1 : -1) - i * s0));

        if (vehicle.isInWater() || vehicle.onGround()) {
            double water = (!vehicle.isInWater() && !vehicle.onGround() ? 0.05f : (vehicle.isInWater() && !vehicle.onGround() ? 0.3f : 1));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.15 * water * vehicle.targetSpeed * vehicle.getEntityData().get(POWER))));
        }
    }

    public static void shipEngine(VehicleEntity vehicle, EngineInfo.Ship engineInfo) {
        double buoyancy = engineInfo.buoyancy;
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));
        float maxForwardSpeedRate = engineInfo.maxForwardSpeedRate;
        float maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate;
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float steeringSpeed = engineInfo.steeringSpeed;
        double bodyPitchRate = engineInfo.bodyPitchRate;
        double bodyRollRate = engineInfo.bodyRollRate;

        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, fluidFloat, 0));
        }

        if (vehicle.onGround()) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.2, 0.99, 0.2));
        } else if (vehicle.isInWater()) {
            float f = (float) (0.75f - (0.04f * java.lang.Math.min(VehicleVecUtils.getSubmergedHeight(vehicle), vehicle.getBbHeight())) + 0.09f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.04 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f, 0.85, f));
        } else {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (vehicle.level() instanceof ServerLevel serverLevel && vehicle.isInWater() && vehicle.getDeltaMovement().length() > 0.1) {
            double y = vehicle.getY() + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2;
            sendParticle(serverLevel, ParticleTypes.CLOUD, vehicle.getX() + 0.5 * vehicle.getDeltaMovement().x, y, vehicle.getZ() + 0.5 * vehicle.getDeltaMovement().z, (int) (2 + 4 * vehicle.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, vehicle.getX() + 0.5 * vehicle.getDeltaMovement().x, y, vehicle.getZ() + 0.5 * vehicle.getDeltaMovement().z, (int) (2 + 10 * vehicle.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, vehicle.getX() - 4.5 * vehicle.getLookAngle().x, vehicle.getY() - 0.25, vehicle.getZ() - 4.5 * vehicle.getLookAngle().z, (int) (40 * Mth.abs(vehicle.getEntityData().get(POWER))), 0.15, 0.15, 0.15, 0.02, true);
        }

        Entity passenger0 = vehicle.getFirstPassenger();

        if (vehicle.getEnergy() > energyCost) {
            if (passenger0 == null) {
                vehicle.setLeftInputDown(false);
                vehicle.setRightInputDown(false);
                vehicle.setForwardInputDown(false);
                vehicle.setBackInputDown(false);
            }

            if (vehicle.forwardInputDown()) {
                vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + (vehicle.getEntityData().get(POWER) < 0 ? powerAdd * 2f : powerAdd), 1));
            }

            if (vehicle.backInputDown()) {
                vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - (vehicle.getEntityData().get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
            }

            if (vehicle.getEntityData().get(POWER) > 0) {
                vehicle.targetSpeed = maxForwardSpeedRate;
            } else {
                vehicle.targetSpeed = maxBackwardSpeedRate;
            }

            if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
                vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.97f);
            }

            if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
                vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.98f);
            }

            if (vehicle.getEntityData().get(MAIN_ENGINE_DAMAGED)) {
                vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.875f);
            }

            if (vehicle.level() instanceof ServerLevel) {
                vehicle.consumeEnergy(energyCost);
            }

            if (vehicle.rightInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - steeringSpeed);
            } else if (vehicle.leftInputDown()) {
                vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + steeringSpeed);
            }

            vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * (float) Math.max(0.78f - 0.25f * vehicle.getDeltaMovement().horizontalDistance(), 0.1));

            vehicle.setPropellerRot(vehicle.getPropellerRot() + 2 * vehicle.getEntityData().get(POWER));
            vehicle.setRudderRot(Mth.clamp(vehicle.getRudderRot() - vehicle.getEntityData().get(DELTA_ROT), -0.8f, 0.8f) * 0.75f);

            if (vehicle.isInWater() || vehicle.isUnderWater()) {
                vehicle.setXRot(vehicle.getXRot() * 0.85f);
                float direct = (90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
                vehicle.setXRot((float) (vehicle.getXRot() - direct * (vehicle.onGround() ? 0 : 1) * bodyPitchRate * vehicle.getDeltaMovement().horizontalDistance()));
                vehicle.setYRot((float) (vehicle.getYRot() - 20 * vehicle.getDeltaMovement().horizontalDistance() * vehicle.getEntityData().get(DELTA_ROT) * (vehicle.getEntityData().get(POWER) > 0 ? 1 : -1)));
                vehicle.setZRot((float) (vehicle.getRoll() - direct * vehicle.getEntityData().get(DELTA_ROT) * (vehicle.onGround() ? 0 : 1) * bodyRollRate * 10 * vehicle.getDeltaMovement().horizontalDistance()));
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.15 * vehicle.targetSpeed * vehicle.getEntityData().get(POWER))));
            } else {
                vehicle.setXRot(vehicle.getXRot() * 0.99f);
            }
        }

        vehicle.setZRot(vehicle.roll * 0.85f);
    }

    public static void helicopterEngine(VehicleEntity vehicle, EngineInfo.Helicopter engineInfo) {
        int energyCost = (int) engineInfo.energyCostRate;
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float pitchSpeed = engineInfo.pitchSpeed;
        float yawSpeed = engineInfo.yawSpeed;
        float rollSpeed = engineInfo.rollSpeed;
        float lift = engineInfo.liftSpeed;

        if (vehicle.onGround()) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.8, 1, 0.8));
        } else {
            vehicle.setZRot(vehicle.getRoll() * (vehicle.backInputDown() ? 0.9f : 0.99f));
            float f = (float) Mth.clamp(0.95f - 0.015 * vehicle.getDeltaMovement().length() + 0.02f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90, 0.01, 0.99);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale((vehicle.getXRot() < 0 ? -0.035 : (vehicle.getXRot() > 0 ? 0.035 : 0)) * vehicle.getDeltaMovement().length())));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f, 0.95, f));
        }

        if (vehicle.isInWater() && vehicle.tickCount % 4 == 0 && VehicleVecUtils.getSubmergedHeight(vehicle) > 0.5 * vehicle.getBbHeight()) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            vehicle.hurt(ModDamageTypes.causeVehicleStrikeDamage(vehicle.level().registryAccess(), vehicle, vehicle.getFirstPassenger() == null ? vehicle : vehicle.getFirstPassenger()), 6 + (float) (20 * ((vehicle.lastTickSpeed - 0.4) * (vehicle.lastTickSpeed - 0.4))));
        }

        Entity pilot = vehicle.getFirstPassenger();

        boolean hasPassenger = false;

        for (int i = 0; i < vehicle.getMaxPassengers() - 1; i++) {
            if (vehicle.getNthEntity(i) != null) {
                hasPassenger = true;
            }
        }

        float diffX;
        float diffZ;

        if (vehicle.getHealth() > 0.1f * vehicle.getMaxHealth()) {
            var landingPos = findNearestLandingPos(vehicle, 30);
            if (pilot == null) {
                vehicle.setLeftInputDown(false);
                vehicle.setRightInputDown(false);
                vehicle.setForwardInputDown(false);
                vehicle.setBackInputDown(false);
                vehicle.setUpInputDown(false);
                vehicle.setDownInputDown(false);
                vehicle.setZRot(vehicle.roll * 0.98f);
                vehicle.setXRot(vehicle.getXRot() * 0.98f);
                vehicle.getDeltaMovement().multiply(0.96, 0.98,0.96);
                if (hasPassenger) {
                    vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.99f);
                }
            } else {
                if (!vehicle.backInputDown() || landingPos == null) {
                    if (vehicle.rightInputDown()) {
                        vehicle.holdTick++;
                        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - 2f * Math.min(vehicle.holdTick, 7) * vehicle.getEntityData().get(POWER));
                    } else if (vehicle.leftInputDown()) {
                        vehicle.holdTick++;
                        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + 2f * Math.min(vehicle.holdTick, 7) * vehicle.getEntityData().get(POWER));
                    } else {
                        vehicle.holdTick = 0;
                    }
                    vehicle.setXRot(vehicle.getXRot() + ((vehicle.onGround()) ? 0 : 1.5f) * pitchSpeed * vehicle.getMouseMoveSpeedY() * vehicle.getEntityData().get(PROPELLER_ROT));
                    vehicle.setZRot(vehicle.getRoll() - rollSpeed * (vehicle.getEntityData().get(DELTA_ROT) + (vehicle.onGround() ? 0 : 0.25f) * vehicle.getMouseMoveSpeedX() * vehicle.getEntityData().get(PROPELLER_ROT)));
                }

                vehicle.setYRot(vehicle.getYRot() + yawSpeed * Mth.clamp((vehicle.onGround() ? 0.1f : 2f) * vehicle.getMouseMoveSpeedX() * vehicle.getEntityData().get(PROPELLER_ROT) + (vehicle.getEntityData().get(SUB_ENGINE_DAMAGED) ? 25 : 0) * vehicle.getEntityData().get(PROPELLER_ROT), -10f, 10f));
                if (landingPos != null && !vehicle.onGround() && vehicle.backInputDown()) {
                    updateAutoLanding(vehicle, landingPos);
                }

                if (pilot instanceof Player player && vehicle.level().isClientSide && landingPos != null && !vehicle.onGround()) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.press_s_to_landing"), true);
                }
            }

            if (vehicle.getEnergy() > energyCost) {
                boolean up = vehicle.upInputDown() || vehicle.forwardInputDown();
                boolean down = vehicle.downInputDown();

                if (!vehicle.engineStart && up) {
                    vehicle.engineStart = true;
                    vehicle.level().playSound(null, vehicle, engineInfo.engineStartSound, vehicle.getSoundSource(), 3, 1);
                }

                if (up && vehicle.engineStartOver) {
                    vehicle.holdPowerTick++;
                    vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + 0.0007f * powerAdd * Math.min(vehicle.holdPowerTick, 10), 0.12f));
                }

                if (vehicle.engineStartOver) {
                    if (down) {
                        vehicle.holdPowerTick++;
                        vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.001f * powerReduce * Math.min(vehicle.holdPowerTick, 5), vehicle.onGround() ? 0 : 0.025f / lift));
                    } else if (vehicle.backInputDown()) {
                        vehicle.holdPowerTick++;
                        vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.001f * powerReduce * Math.min(vehicle.holdPowerTick, 5), vehicle.onGround() ? 0 : 0.058f / lift));
                    }
                }

                if (vehicle.engineStart && !vehicle.engineStartOver) {
                    vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + 0.0012f * powerAdd, 0.045f));
                }

                if (!(up || down || vehicle.backInputDown()) && vehicle.engineStartOver) {
                    if (vehicle.getDeltaMovement().y() < 0) {
                        vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + 0.0002f, 0.12f));
                    } else {
                        vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - (vehicle.onGround() ? 0.00005f : 0.0002f), 0));
                    }
                    vehicle.holdPowerTick = 0;
                }
            } else {
                vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.0001f, 0));
                vehicle.setForwardInputDown(false);
                vehicle.setBackInputDown(false);
                vehicle.engineStart = false;
                vehicle.engineStartOver = false;
            }
        } else if (!vehicle.onGround() && vehicle.engineStartOver) {
            vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.0003f, 0.01f));
            vehicle.destroyRot += 0.08f;

            diffX = 45 - vehicle.getXRot();
            diffZ = -20 - vehicle.getRoll();

            vehicle.setXRot(vehicle.getXRot() + diffX * 0.05f * vehicle.getEntityData().get(PROPELLER_ROT));
            vehicle.setYRot(vehicle.getYRot() + vehicle.destroyRot);
            vehicle.setZRot(vehicle.getRoll() + diffZ * 0.1f * vehicle.getEntityData().get(PROPELLER_ROT));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, -vehicle.destroyRot * 0.004, 0));
        }

        if (vehicle.getEntityData().get(MAIN_ENGINE_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.98f);
        }

        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * 0.9f);
        vehicle.getEntityData().set(PROPELLER_ROT, Mth.lerp(0.18f, vehicle.getEntityData().get(PROPELLER_ROT), vehicle.getEntityData().get(POWER)));
        vehicle.setPropellerRot(vehicle.getPropellerRot() + 30 * vehicle.getEntityData().get(PROPELLER_ROT));
        vehicle.getEntityData().set(PROPELLER_ROT, vehicle.getEntityData().get(PROPELLER_ROT) * 0.9995f);

        if (vehicle.engineStart) {
            vehicle.consumeEnergy((int) (energyCost * 8.3333f * Mth.abs(vehicle.getEntityData().get(POWER))));
        }

        Vec3 force = vehicle.getUpVec(1);

        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(force.scale(vehicle.getEntityData().get(PROPELLER_ROT) * lift)));

        if (vehicle.getEntityData().get(POWER) > 0.04f) {
            vehicle.engineStartOver = true;
        }

        if (vehicle.getEntityData().get(POWER) < 0.0004f) {
            vehicle.engineStart = false;
            vehicle.engineStartOver = false;
        }
    }

    public static void aircraftEngine(VehicleEntity vehicle, EngineInfo.AirCraft engineInfo) {
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float pitchSpeed = engineInfo.pitchSpeed;
        float yawSpeed = engineInfo.yawSpeed;
        float rollSpeed = engineInfo.rollSpeed;
        float lift = engineInfo.liftSpeed;
        float speedRate = engineInfo.speedRate;
        float gearRotateAngle = engineInfo.gearRotateAngle;
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));

        float f = (float) Mth.clamp(Math.max((vehicle.onGround() ? 0.819f : 0.82f) - 0.005 * vehicle.getDeltaMovement().length(), 0.5) + 0.001f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)) > 0;
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale((forward ? 0.227 : 0.1) * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f, f, f));

        if (vehicle.isInWater() && vehicle.tickCount % 4 == 0) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (vehicle.lastTickSpeed > 0.4) {
                vehicle.hurt(ModDamageTypes.causeVehicleStrikeDamage(vehicle.level().registryAccess(), vehicle, vehicle.getFirstPassenger() == null ? vehicle : vehicle.getFirstPassenger()), (float) (20 * ((vehicle.lastTickSpeed - 0.4) * (vehicle.lastTickSpeed - 0.4))));
            }
        }

        Entity passenger = vehicle.getFirstPassenger();

        if (vehicle.getHealth() > 0.1f * vehicle.getMaxHealth()) {
            if (passenger == null || vehicle.isInWater()) {
                vehicle.setLeftInputDown(false);
                vehicle.setRightInputDown(false);
                vehicle.setForwardInputDown(false);
                vehicle.setBackInputDown(false);
                vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.95f);
                if (vehicle.onGround()) {
                    vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.94, 1, 0.94));
                } else {
                    vehicle.setXRot(Mth.clamp(vehicle.getXRot() + 0.1f, -89, 89));
                }
            } else if (passenger instanceof Player) {
                if (vehicle.getEnergy() > energyCost) {
                    if (!vehicle.engineStart && vehicle.forwardInputDown()) {
                        vehicle.engineStart = true;
                        if (vehicle.getEntityData().get(POWER) > 0) {
                            vehicle.level().playSound(null, vehicle, engineInfo.engineStartSound, vehicle.getSoundSource(), 3, 1);
                        }
                    }

                    if (vehicle.forwardInputDown()) {
                        vehicle.getEntityData().set(POWER, (float) Mth.clamp(vehicle.getEntityData().get(POWER) + 0.0045f * powerAdd, -0.1, 1));
                    }

                    if (vehicle.backInputDown()) {
                        vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.006f * powerReduce, vehicle.onGround() ? -0.2f : 0.4f));
                    }
                }

                if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
                    vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.995f);
                }

                if (!vehicle.onGround()) {
                    if (vehicle.rightInputDown()) {
                        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) - 0.6f);
                    } else if (vehicle.leftInputDown()) {
                        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) + 0.6f);
                    }
                }

                // 刹车
                if (vehicle.downInputDown()) {
                    if (vehicle.onGround()) {
                        vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.92f);
                        vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.97, 1, 0.97));
                    } else {
                        vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.97f);
                        vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.994, 1, 0.994));
                    }
                    vehicle.getEntityData().set(PLANE_BREAK, Math.min(vehicle.getEntityData().get(PLANE_BREAK) + 10, 60f));
                }
            }

            if (vehicle.engineStart) {
                vehicle.consumeEnergy(energyCost);
            }

            float rotSpeed = 1.5f + 1.2f * Mth.abs(VectorTool.calculateY(vehicle.getRoll()));

            float addY = Mth.clamp(Math.max((vehicle.onGround() ? 0.6f : 0.2f) * (float) vehicle.getDeltaMovement().length(), 0f) * vehicle.getMouseMoveSpeedX(), -rotSpeed, rotSpeed);
            float addX = Mth.clamp(Math.min((float) Math.max(vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)) - 0.24, 0.15), 0.4f) * vehicle.getMouseMoveSpeedY(), -3.5f, 3.5f);
            float addZ = vehicle.getEntityData().get(DELTA_ROT) - (vehicle.onGround() ? 0 : 0.004f) * vehicle.getMouseMoveSpeedX() * (float) vehicle.getDeltaMovement().dot(vehicle.getViewVector(1));

            vehicle.setYRot(vehicle.getYRot() + yawSpeed * addY);
            if (!vehicle.onGround()) {
                vehicle.setXRot(vehicle.getXRot() + pitchSpeed * addX);
                vehicle.setZRot(vehicle.getRoll() - rollSpeed * addZ);
            }

            // 自动回正
            if (!vehicle.onGround()) {
                float xSpeed = 1 + 20 * Mth.abs(vehicle.getXRot() / 180);
                float speed = Mth.clamp(Mth.abs(vehicle.roll) / (90 / xSpeed), 0, 1);

                if (vehicle.roll > 0) {
                    vehicle.setZRot(vehicle.roll - Math.min(speed, vehicle.roll));
                } else if (vehicle.roll < 0) {
                    vehicle.setZRot(vehicle.roll + Math.min(speed, -vehicle.roll));
                }
            }

            vehicle.setPropellerRot(vehicle.getPropellerRot() + 30 * vehicle.getEntityData().get(POWER));

            // 起落架
            if (vehicle.upInputDown()) {
                vehicle.setUpInputDown(false);
                if (vehicle.getEntityData().get(GEAR_ROT) == 0 && !vehicle.onGround()) {
                    vehicle.getEntityData().set(GEAR_UP, true);
                } else if (vehicle.getEntityData().get(GEAR_ROT) == 1) {
                    vehicle.getEntityData().set(GEAR_UP, false);
                }
            }

            if (vehicle.onGround()) {
                vehicle.getEntityData().set(GEAR_UP, false);
            }

            if (vehicle.getEntityData().get(GEAR_UP)) {
                vehicle.getEntityData().set(GEAR_ROT, Math.min(vehicle.getEntityData().get(GEAR_ROT) + 0.05f, 1));
            } else {
                vehicle.getEntityData().set(GEAR_ROT, Math.max(vehicle.getEntityData().get(GEAR_ROT) - 0.05f, 0));
            }

            vehicle.setGearRot(vehicle.getEntityData().get(GEAR_ROT) * gearRotateAngle);

            float flapX = (1 - (Mth.abs(vehicle.getRoll())) / 90) * Mth.clamp(vehicle.getMouseMoveSpeedY(), -22.5f, 22.5f) - VectorTool.calculateY(vehicle.getRoll()) * Mth.clamp(vehicle.getMouseMoveSpeedX(), -22.5f, 22.5f);

            vehicle.setFlap1LRot(Mth.clamp(-flapX - 4 * addZ - vehicle.getEntityData().get(PLANE_BREAK), -22.5f, 22.5f));
            vehicle.setFlap1RRot(Mth.clamp(-flapX + 4 * addZ - vehicle.getEntityData().get(PLANE_BREAK), -22.5f, 22.5f));
            vehicle.setFlap1L2Rot(Mth.clamp(-flapX - 4 * addZ + vehicle.getEntityData().get(PLANE_BREAK), -22.5f, 22.5f));
            vehicle.setFlap1R2Rot(Mth.clamp(-flapX + 4 * addZ + vehicle.getEntityData().get(PLANE_BREAK), -22.5f, 22.5f));

            vehicle.setFlap2LRot(Mth.clamp(flapX - 4 * addZ, -22.5f, 22.5f));
            vehicle.setFlap2RRot(Mth.clamp(flapX + 4 * addZ, -22.5f, 22.5f));

            float flapY = (1 - (Mth.abs(vehicle.getRoll())) / 90) * Mth.clamp(vehicle.getMouseMoveSpeedX(), -22.5f, 22.5f) + VectorTool.calculateY(vehicle.getRoll()) * Mth.clamp(vehicle.getMouseMoveSpeedY(), -22.5f, 22.5f);
            vehicle.setFlap3Rot(flapY * 5);

        } else if (!vehicle.onGround()) {
            float diffX;
            vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - 0.0003f, 0.02f));
            vehicle.destroyRot += 0.1f;
            diffX = 90 - vehicle.getXRot();
            vehicle.setXRot(vehicle.getXRot() + diffX * 0.001f * vehicle.destroyRot);
            vehicle.setZRot(vehicle.getRoll() - vehicle.destroyRot);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, -0.03, 0));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, -vehicle.destroyRot * 0.005, 0));
        }

        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * 0.85f);
        vehicle.getEntityData().set(PLANE_BREAK, vehicle.getEntityData().get(PLANE_BREAK) * 0.8f);
        if (vehicle.onGround()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.995f);
        }

        if (vehicle.getEntityData().get(MAIN_ENGINE_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        if (vehicle.getEntityData().get(SUB_ENGINE_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        double flapAngle = (vehicle.getFlap1LRot() + vehicle.getFlap1RRot() + vehicle.getFlap1L2Rot() + vehicle.getFlap1R2Rot()) / 4;
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getUpVec(1).scale(vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)) * 0.022 * lift * (1 + Math.sin((vehicle.onGround() ? 25 : flapAngle + 25) * Mth.DEG_TO_RAD)))));
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.03 * speedRate * vehicle.getEntityData().get(POWER) * (vehicle.sprintInputDown() ? 2.2 : 1))));

        if (vehicle.getEntityData().get(POWER) > 0.2f) {
            vehicle.engineStartOver = true;
        }

        if (vehicle.getEntityData().get(POWER) < 0.0004f) {
            vehicle.engineStart = false;
            vehicle.engineStartOver = false;
        }
    }

    public static void wheelChairEngine(VehicleEntity vehicle, EngineInfo.WheelChair engineInfo) {
        double buoyancy = engineInfo.buoyancy;
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));
        double wheelRotSpeed = engineInfo.wheelRotSpeed;
        float wheelDifferential = (float) engineInfo.wheelDifferential;
        float maxForwardSpeedRate = engineInfo.maxForwardSpeedRate;
        float maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate;
        float powerAdd = engineInfo.increment;
        float powerReduce = engineInfo.decrement;
        float steeringSpeed = engineInfo.steeringSpeed;
        float bodyRollRate = (float) engineInfo.bodyRollRate;
        int jumpEnergyCost = engineInfo.jumpEnergyCost;

        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0, fluidFloat, 0));
        }

        if (vehicle.onGround()) {
            float f0 = 0.63f + 0.25f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.05 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f0, 0.99, f0));
        } else if (vehicle.isInWater()) {
            float f1 = 0.74f + 0.09f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).normalize().scale(0.04 * vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)))));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(f1, 0.85, f1));
        } else {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }
        vehicle.setSprinting(vehicle.getDeltaMovement().horizontalDistance() > 0.15);

        Entity passenger0 = vehicle.getFirstPassenger();
        float diffY = 0;

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false);
            vehicle.setRightInputDown(false);
            vehicle.setForwardInputDown(false);
            vehicle.setBackInputDown(false);
            vehicle.getEntityData().set(POWER, 0f);
        } else {
            diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger0.getYHeadRot() - vehicle.getYRot()));
            vehicle.setYRot(vehicle.getYRot() + Mth.clamp(0.4f * diffY, -5f * steeringSpeed, 5f * steeringSpeed));

            float direct = (90 - (float) VehicleVecUtils.calculateAngle(vehicle.getDeltaMovement(), vehicle.getViewVector(1))) / 90;
            vehicle.setZRot((float) (vehicle.getRoll() + direct * diffY * 0.1 * bodyRollRate * vehicle.getDeltaMovement().length()));
        }

        if (vehicle.forwardInputDown()) {
            if (vehicle.getEnergy() <= 0 && passenger0 instanceof Player player) {
                moveWithOutPower(vehicle, player, true);
            } else {
                vehicle.getEntityData().set(POWER, Math.min(vehicle.getEntityData().get(POWER) + (vehicle.getEntityData().get(POWER) < 0 ? powerAdd * 2f : powerAdd), (vehicle.sprintInputDown() ? 2f : 1f)));
            }
        }

        if (vehicle.backInputDown()) {
            if (vehicle.getEnergy() <= 0 && passenger0 instanceof Player player) {
                moveWithOutPower(vehicle, player, false);
            } else {
                vehicle.getEntityData().set(POWER, Math.max(vehicle.getEntityData().get(POWER) - (vehicle.getEntityData().get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
            }
        }

        if (vehicle.getEntityData().get(POWER) > 0) {
            vehicle.targetSpeed = maxForwardSpeedRate * (1 + vehicle.getXRot() / 55);
        } else {
            vehicle.targetSpeed = maxBackwardSpeedRate * (1 - vehicle.getXRot() / 55);
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        if (vehicle.upInputDown() && vehicle.onGround() && vehicle.getEnergy() > jumpEnergyCost && vehicle.jumpCoolDown == 0 && engineInfo.canJump) {
            if (passenger0 instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.WHEEL_CHAIR_JUMP.get(), SoundSource.PLAYERS, 1, 1);
            }
            vehicle.consumeEnergy(jumpEnergyCost);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getUpVec(1).scale(engineInfo.jumpForce)));
            vehicle.jumpCoolDown = engineInfo.jumpCoolDown;
        }

        if (vehicle.level() instanceof ServerLevel) {
            vehicle.consumeEnergy(energyCost);
        }

        double s0 = vehicle.getDeltaMovement().dot(vehicle.getViewVector(1));
        vehicle.setLeftWheelRot((float) (vehicle.getLeftWheelRot() - 1.25 * wheelRotSpeed * s0) - 0.015f * wheelDifferential * Mth.clamp(0.4f * diffY, -5f, 5f));
        vehicle.setRightWheelRot((float) (vehicle.getRightWheelRot() - 1.25 * wheelRotSpeed * s0) + 0.015f * wheelDifferential * Mth.clamp(0.4f * diffY, -5f, 5f));

        if (vehicle.isInWater() || vehicle.onGround()) {
            double water = (!vehicle.isInWater() && !vehicle.onGround() ? 0.05f : (vehicle.isInWater() && !vehicle.onGround() ? 0.3f : 1));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.08 * water * vehicle.targetSpeed * vehicle.getEntityData().get(POWER))));
        }
    }

    public static void moveWithOutPower(VehicleEntity vehicle, Player player, boolean forward) {
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(forward ? 0.1f : -0.1f)));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.BOAT_PADDLE_LAND, SoundSource.PLAYERS, 1, 1);
        }
        player.causeFoodExhaustion(0.03F);

        vehicle.setForwardInputDown(false);
        vehicle.setBackInputDown(false);
    }

    /**
     * 查找实体下方半球区域内最近的降落辅助方块位置
     *
     * @param radius 搜索半径
     * @return 辅助方块顶面位置，如果未找到则返回null
     */
    public static Vec3 findNearestLandingPos(VehicleEntity entity, int radius) {
        Level world = entity.level();
        BlockPos entityPos = entity.blockPosition();
        List<BlockPos> landingBlocks = new ArrayList<>();

        // 遍历半球区域内的所有方块
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= 0; y++) { // 只检查实体下方的区域
                    // 检查是否在半球内 (x² + y² + z² ≤ r²)
                    if (x * x + y * y + z * z <= radius * radius) {
                        BlockPos checkPos = entityPos.offset(x, y, z);

                        // 检查是否为降落辅助方块
                        if (world.getBlockState(checkPos).is(ModTags.Blocks.AUTO_LANDING)) {
                            landingBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // 如果没有找到降落辅助方块，返回null
        if (landingBlocks.isEmpty()) {
            return null;
        }

        // 按距离排序，找到最近的降落辅助方块
        landingBlocks.sort(Comparator.comparingDouble(pos ->
                entity.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)));

        return landingBlocks.getFirst().getCenter();
    }

    public static void updateAutoLanding(VehicleEntity entity, Vec3 landingTarget) {
        // 计算水平方向上的偏移向量 (忽略Y轴)
        Vec3 currentPos = entity.position();
        Vec3 horizontalOffset = new Vec3(
                landingTarget.x - currentPos.x,
                0,
                landingTarget.z - currentPos.z
        );

        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.975, 0.99, 0.975));

        // 计算距离和方向
        double horizontalDistance = horizontalOffset.length();
        Vec3 horizontalDirection = horizontalDistance > 0 ?
                horizontalOffset.normalize() : Vec3.ZERO;


        // 倾斜平滑因子
        float tiltSmoothingFactor = 0.1f;

        double horizontalDistanceNew = horizontalDistance - 5 * entity.getDeltaMovement().horizontalDistance();

        // 计算需要的倾斜角度 (与距离成正比，但有最大限制)
        // 直升机辅助降落这一块
        // 最大倾斜角度(度)
        float maxTiltAngle = 15.0f;
        float targetTilt = (float) Math.min(maxTiltAngle, horizontalDistanceNew * 2);

        // 将世界方向转换为本地倾斜方向
        // 需要考虑直升机的当前偏航角(yRot)
        float yawRad = Math.toRadians(-entity.getYRot());
        Vec3 localDirection = new Vec3(
                horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
                0,
                horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        );

        // 计算目标俯仰和滚转
        float targetXRot = (float) (-localDirection.z * targetTilt);
        float targetZRot = (float) (localDirection.x * targetTilt);

        // 平滑过渡到目标姿态
        entity.setXRot(lerpAngle(entity.getXRot(), -targetXRot, tiltSmoothingFactor));
        entity.setZRot(lerpAngle(entity.getRoll(), -targetZRot, tiltSmoothingFactor));
    }

    // 角度线性插值方法
    private static float lerpAngle(float current, float target, float factor) {
        // 处理角度环绕
        float diff = target - current;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;

        return current + diff * factor;
    }
}
