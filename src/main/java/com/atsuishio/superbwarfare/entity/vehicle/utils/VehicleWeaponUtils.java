package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;
import static com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition;

/**
 * 用于处理载具武器瞄准或其他战斗相关方法的工具类
 */
public final class VehicleWeaponUtils {

    /**
     * 根据操控者调整载具炮塔角度
     *
     * @param vehicle 载具
     */
    public static void adjustTurretAngle(VehicleEntity vehicle) {
        float ySpeed = vehicle.getTurretTurnYSpeed();
        float xSpeed = vehicle.getTurretTurnXSpeed();

        Entity driver = vehicle.getNthEntity(vehicle.getTurretControllerIndex());
        if (driver == null) {
            vehicle.turretYRotLock = 0;
        } else {
            float turretAngle = -Mth.wrapDegrees(driver.getYHeadRot() - vehicle.getYRot());

            float diffY = Mth.wrapDegrees(turretAngle - vehicle.getTurretYRot());
            float diffX = Mth.wrapDegrees(driver.getXRot() - vehicle.getTurretXRot());

            vehicle.turretTurnSound(diffX, diffY, 0.95f);

            if (vehicle.getEntityData().get(TURRET_DAMAGED)) {
                ySpeed *= 0.2f;
                xSpeed *= 0.2f;
            }

            float min = -ySpeed + (float) (vehicle.isInWater() && !vehicle.onGround() ? 2.5 : 6) * vehicle.getEntityData().get(DELTA_ROT);
            float max = ySpeed + (float) (vehicle.isInWater() && !vehicle.onGround() ? 2.5 : 6) * vehicle.getEntityData().get(DELTA_ROT);

            vehicle.setTurretXRot(Mth.clamp(vehicle.getTurretXRot() + Mth.clamp(0.95f * diffX, -xSpeed, xSpeed), -89.5f, 89.5f));
            vehicle.setTurretYRot(vehicle.getTurretYRot() + Mth.clamp(0.9f * diffY, min, max));
            vehicle.turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        }
    }

    /**
     * 根据方向向量，使炮塔自动瞄准
     *
     * @param shootVec 需要让炮塔以这个角度发射的向量
     */
    public static void turretAutoAimFromVector(VehicleEntity vehicle, Vec3 shootVec) {
        float ySpeed = vehicle.getTurretTurnYSpeed();
        float xSpeed = vehicle.getTurretTurnXSpeed();

        var barrelVector = vehicle.getBarrelVector(1);
        float diffY = (float) Mth.wrapDegrees(-VehicleVecUtils.getYRotFromVector(shootVec) + VehicleVecUtils.getYRotFromVector(barrelVector));
        float diffX = (float) Mth.wrapDegrees(-VehicleVecUtils.getXRotFromVector(shootVec) + VehicleVecUtils.getXRotFromVector(barrelVector));

        vehicle.turretTurnSound(diffX, diffY, 0.95f);

        if (vehicle.getEntityData().get(TURRET_DAMAGED)) {
            ySpeed *= 0.2f;
            xSpeed *= 0.2f;
        }

        float v = (float) (vehicle.isInWater() && !vehicle.onGround() ? 2.5 : 6) * vehicle.getEntityData().get(DELTA_ROT);
        float min = -ySpeed + v;
        float max = ySpeed + v;

        vehicle.setTurretXRot(Mth.clamp(vehicle.getTurretXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -vehicle.getTurretMaxPitch(), -vehicle.getTurretMinPitch()));
        vehicle.setTurretYRot(Mth.clamp(vehicle.getTurretYRot() - Mth.clamp(0.5f * diffY, min, max), -vehicle.getTurretMaxYaw(), -vehicle.getTurretMinYaw()));
        vehicle.turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        vehicle.aiTurretDiff = VectorTool.calculateAngle(shootVec, barrelVector);
    }

    /**
     * 根据UUID，使炮塔自动瞄准
     *
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    public static void turretAutoAimFromUuid(VehicleEntity vehicle, String uuid, LivingEntity pLiving) {
        Entity target = EntityFindUtil.findEntity(vehicle.level(), uuid);
        if (target == null) return;

        if (target.getVehicle() != null) {
            target = target.getVehicle();
        }

        Vec3 targetPos = target.getBoundingBox().getCenter();
        Vec3 targetVel = target.getDeltaMovement();

        if (target instanceof LivingEntity living) {
            double gravity = living.getAttributeValue(Attributes.GRAVITY);
            targetVel = targetVel.add(0, gravity, 0);
        }

        if (target instanceof Player) {
            targetVel = targetVel.multiply(2, 1, 2);
        }

        Vec3 targetVec = RangeTool.calculateFiringSolution(vehicle.getShootPos(pLiving, 1), targetPos, targetVel, vehicle.projectileVelocity(pLiving), vehicle.projectileGravity(pLiving));
        vehicle.turretAutoAimFromVector(targetVec);

        int rpm = 20 / Mth.clamp((vehicle.vehicleWeaponRpm(pLiving) / 60), 1, 2147483647);
        if (vehicle.tickCount % rpm == 0) {
            vehicle.aiTurretShoot(pLiving);
        }
    }

    /**
     * 发射烟雾诱饵
     *
     * @param vehicle 载具
     * @param vec3    发射方向
     */
    public static void releaseSmokeDecoy(VehicleEntity vehicle, Vec3 vec3) {
        if (vehicle.decoyInputDown()) {
            if (vehicle.getEntityData().get(DECOY_READY) && vehicle.level() instanceof ServerLevel) {
                for (int i = 0; i < 8; i++) {
                    SmokeDecoyEntity smokeDecoyEntity = new SmokeDecoyEntity(vehicle.level());
                    smokeDecoyEntity.setPos(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight(), vehicle.getZ());
                    smokeDecoyEntity.decoyShoot(vehicle, vec3.yRot((-78.75f + 22.5F * i) * Mth.DEG_TO_RAD), 4f, 8);
                    vehicle.level().addFreshEntity(smokeDecoyEntity);
                }

                vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELEASE.get(), vehicle.getSoundSource(), 1, 1);
                vehicle.decoyReloadCoolDown = 500;
                vehicle.getEntityData().set(DECOY_READY, false);
            }
            vehicle.setDecoyInputDown(false);
        }

        if (!vehicle.getEntityData().get(DECOY_READY) && vehicle.decoyReloadCoolDown == 0 && vehicle.level() instanceof ServerLevel) {
            vehicle.getEntityData().set(DECOY_READY, true);
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.getSoundSource(), 1, 1);
            vehicle.decoyReloadCoolDown = 500;
        }
    }

    /**
     * 发射热诱弹
     *
     * @param vehicle 载具
     */
    public static void releaseDecoy(VehicleEntity vehicle) {
        if (vehicle.decoyInputDown()) {
            if (vehicle.getEntityData().get(DECOY_READY) && vehicle.level() instanceof ServerLevel) {
                for (int i = 0; i < 48; i += 4) {
                    Mod.queueServerWork(i, () -> {
                        Matrix4f transform = vehicle.getVehicleTransform(1);

                        Vector4f worldPositionO = transformPosition(transform, 0, 0, 0);
                        Vector4f worldPosition = transformPosition(transform, 1, -0.2f, 0.6f);
                        Vector4f worldPosition2 = transformPosition(transform, -1, -0.2f, 0.6f);

                        Vec3 shootVecO = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
                        Vec3 shootVec1 = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
                        Vec3 shootVec2 = new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z);

                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec1).normalize());
                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec2).normalize());
                    });
                }

                vehicle.decoyReloadCoolDown = 400;
                vehicle.getEntityData().set(DECOY_READY, false);
            }
            vehicle.setDecoyInputDown(false);
        }
        if (!vehicle.getEntityData().get(DECOY_READY) && vehicle.decoyReloadCoolDown == 0 && vehicle.level() instanceof ServerLevel) {
            vehicle.getEntityData().set(DECOY_READY, true);
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.getSoundSource(), 1, 1);
            vehicle.decoyReloadCoolDown = 400;
        }
    }

    public static void shootDecoy(VehicleEntity vehicle, Vec3 shootVec) {
        FlareDecoyEntity flareDecoyEntity = new FlareDecoyEntity(vehicle.level());

        flareDecoyEntity.setPos(vehicle.getX() + vehicle.getDeltaMovement().x, vehicle.getY() + 0.5 + vehicle.getDeltaMovement().y, vehicle.getZ() + vehicle.getDeltaMovement().z);
        flareDecoyEntity.decoyShoot(vehicle, shootVec, (float) (vehicle.getDeltaMovement().length() * 0.3f + 0.7), 8);

        vehicle.level().addFreshEntity(flareDecoyEntity);
        vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELEASE.get(), vehicle.getSoundSource(), 2, 1);
    }
}
