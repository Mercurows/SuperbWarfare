package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static com.atsuishio.superbwarfare.entity.vehicle.base.HelicopterAutoLandingSystem.findNearestLandingPos;
import static com.atsuishio.superbwarfare.entity.vehicle.base.HelicopterAutoLandingSystem.updateAutoLanding;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

// TODO 等一个优化方案
public final class VehicleEngineUtils {

    public static void trackEngine(VehicleEntity vehicle, double buoyancy, int energyCost, double wheelRotSpeed, double wheelDifferential, double trackSpeed, double trackDifferential, float maxForwardSpeedRate, float maxBackwardSpeedRate, float powerAdd, float powerReduce, float steeringSpeed) {
        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0.0, fluidFloat, 0.0));
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

        if (vehicle.getEnergy() <= 0) return;

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

        if (vehicle.getEntityData().get(ENGINE1_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.96f);
        }

        vehicle.setYRot((float) (vehicle.getYRot() - (vehicle.isInWater() && !vehicle.onGround() ? 2.5 : 6) * vehicle.getEntityData().get(DELTA_ROT) - i * s0));
        if (vehicle.isInWater() || vehicle.onGround()) {
            double water = (!vehicle.isInWater() && !vehicle.onGround() ? 0.05f : (vehicle.isInWater() && !vehicle.onGround() ? 0.3f : 1));
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vehicle.getViewVector(1).scale(0.15 * water * vehicle.targetSpeed * vehicle.getEntityData().get(POWER))));
        }
    }

    public static void wheelEngine(VehicleEntity vehicle, double buoyancy, int energyCost, double wheelRotSpeed, double wheelDifferential, float maxForwardSpeedRate, float maxBackwardSpeedRate, float powerAdd, float powerReduce, float steeringSpeed) {
        if (buoyancy != 0) {
            double fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(0.0, fluidFloat, 0.0));
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

        if (vehicle.getEnergy() <= 0) return;

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

        if (vehicle.getEntityData().get(ENGINE1_DAMAGED)) {
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

    public static void helicopterEngine(VehicleEntity vehicle, EngineInfo.Helicopter engineInfo) {
        int energyCost = (int) (engineInfo.energyCostRate * Mth.abs(vehicle.getEntityData().get(POWER)));
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
                if (hasPassenger) {
                    vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.99f);
                }
            } else {
                if (!vehicle.landingInputDown() || landingPos == null) {
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

                vehicle.setYRot(vehicle.getYRot() + yawSpeed * Mth.clamp((vehicle.onGround() ? 0.1f : 2f) * vehicle.getMouseMoveSpeedX() * vehicle.getEntityData().get(PROPELLER_ROT) + (vehicle.getEntityData().get(ENGINE2_DAMAGED) ? 25 : 0) * vehicle.getEntityData().get(PROPELLER_ROT), -10f, 10f));
                if (landingPos != null && !vehicle.onGround() && vehicle.landingInputDown()) {
                    updateAutoLanding(vehicle, landingPos);
                }

                if (pilot instanceof Player player && vehicle.level().isClientSide && landingPos != null && !vehicle.onGround()) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.press_s_to_landing"), true);
                }
            }

            if (vehicle.getEnergy() > 0) {
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

        if (vehicle.getEntityData().get(ENGINE1_DAMAGED)) {
            vehicle.getEntityData().set(POWER, vehicle.getEntityData().get(POWER) * 0.98f);
        }

        vehicle.getEntityData().set(DELTA_ROT, vehicle.getEntityData().get(DELTA_ROT) * 0.9f);
        vehicle.getEntityData().set(PROPELLER_ROT, Mth.lerp(0.18f, vehicle.getEntityData().get(PROPELLER_ROT), vehicle.getEntityData().get(POWER)));
        vehicle.setPropellerRot(vehicle.getPropellerRot() + 30 * vehicle.getEntityData().get(PROPELLER_ROT));
        vehicle.getEntityData().set(PROPELLER_ROT, vehicle.getEntityData().get(PROPELLER_ROT) * 0.9995f);

        if (vehicle.engineStart) {
            vehicle.consumeEnergy((int) (energyCost * vehicle.getEntityData().get(POWER) * 8.3333f));
        }

        Matrix4f transform = vehicle.getVehicleTransform(1);

        Vector4f force0 = vehicle.transformPosition(transform, 0, 0, 0);
        Vector4f force1 = vehicle.transformPosition(transform, 0, 1, 0);

        Vec3 force = new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));

        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(force.scale(vehicle.getEntityData().get(PROPELLER_ROT) * lift)));

        if (vehicle.getEntityData().get(POWER) > 0.04f) {
            vehicle.engineStartOver = true;
        }

        if (vehicle.getEntityData().get(POWER) < 0.0004f) {
            vehicle.engineStart = false;
            vehicle.engineStartOver = false;
        }
    }
}
