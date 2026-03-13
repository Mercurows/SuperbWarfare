package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.RangeTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector4d;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.TURRET_DAMAGED;
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
        if (vehicle.isWreck()) return;
        Entity driver = vehicle.getNthEntity(vehicle.getTurretControllerIndex());
        var pos = vehicle.getBarrelPosition();
        if (driver != null && pos != null) {
            Vec3 aimPos = vehicle.getBoundingBox().getCenter().add(driver.getViewVector(1).scale(512));

            var transform = vehicle.getTurretTransform(1);
            var worldPosition = transformPosition(transform, pos.x, pos.y, pos.z);

            Vec3 aimVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(aimPos);
            turretAutoAimFromVector(vehicle, aimVec);
        }
    }

    /**
     * 根据方向向量，使炮塔自动瞄准
     *
     * @param shootVec 需要让炮塔以这个角度发射的向量
     */
    public static void turretAutoAimFromVector(VehicleEntity vehicle, Vec3 shootVec) {
        if (vehicle.isWreck()) return;
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

        float min = -ySpeed;
        float max = ySpeed;

        vehicle.setTurretXRot(Mth.clamp(vehicle.getTurretXRot() + Mth.clamp(1f * diffX, -xSpeed, xSpeed), -vehicle.getTurretMaxPitch(), -vehicle.getTurretMinPitch()));
        vehicle.setTurretYRot(Mth.clamp(vehicle.getTurretYRot() - Mth.clamp(1f * diffY, min, max), -vehicle.getTurretMaxYaw(), -vehicle.getTurretMinYaw()));
        vehicle.setTurretYRotLock(Mth.clamp(-1f * diffY, min, max));
    }

    /**
     * 根据UUID，使炮塔自动瞄准
     *
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    public static void turretAutoAimFromUuid(VehicleEntity vehicle, String uuid, LivingEntity pLiving) {
        if (vehicle.isWreck()) return;
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

        Vec3 targetVec = RangeTool.calculateFiringSolution(vehicle.getShootPos(pLiving, 1).subtract(vehicle.getShootVec(pLiving, 1).scale(vehicle.getShootPos(pLiving, 1).distanceTo(pLiving.position()))), targetPos, targetVel, vehicle.getProjectileVelocity(pLiving), vehicle.getProjectileGravity(pLiving));
        vehicle.turretAutoAimFromVector(targetVec);
    }

    /**
     * 发射烟雾诱饵
     *
     * @param vehicle 载具
     * @param vec3    发射方向
     */
    public static void releaseSmokeDecoy(VehicleEntity vehicle, Vec3 vec3) {
        if (vehicle.decoyInputDown()) {
            if (vehicle.getDecoyReady() && vehicle.level() instanceof ServerLevel) {
                for (int i = 0; i < 8; i++) {
                    SmokeDecoyEntity smokeDecoyEntity = new SmokeDecoyEntity(vehicle.level());
                    smokeDecoyEntity.setPos(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight(), vehicle.getZ());
                    smokeDecoyEntity.decoyShoot(vehicle, vec3.yRot((-78.75f + 22.5F * i) * Mth.DEG_TO_RAD), 4f, 8);
                    vehicle.level().addFreshEntity(smokeDecoyEntity);
                }

                vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELEASE.get(), vehicle.getSoundSource(), 1, 1);
                vehicle.setDecoyReloadCoolDown(500);
                vehicle.setDecoyReady(false);
            }
            vehicle.setDecoyInputDown(false);
        }

        if (!vehicle.getDecoyReady() && vehicle.getDecoyReloadCoolDown() == 0 && vehicle.level() instanceof ServerLevel) {
            vehicle.setDecoyReady(true);
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.getSoundSource(), 1, 1);
            vehicle.setDecoyReloadCoolDown(500);
        }
    }

    /**
     * 发射热诱弹
     *
     * @param vehicle 载具
     */
    public static void releaseDecoy(VehicleEntity vehicle) {
        if (vehicle.decoyInputDown()) {
            if (vehicle.getDecoyReady() && vehicle.level() instanceof ServerLevel) {
                for (int i = 0; i < 54; i += 6) {
                    int finalI = i;
                    Mod.queueServerWork(i, () -> {
                        Matrix4d transform = vehicle.getVehicleTransform(1);

                        Vector4d worldPositionO = transformPosition(transform, 0, 0, 0);
                        Vector4d worldPosition = transformPosition(transform, 1, -0.2, 0.6);
                        Vector4d worldPosition2 = transformPosition(transform, -1, -0.2, 0.6);

                        Vec3 shootVecO = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
                        Vec3 shootVec1 = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
                        Vec3 shootVec2 = new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z);

                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec1).normalize(), finalI == 6);
                        shootDecoy(vehicle, shootVecO.vectorTo(shootVec2).normalize(), finalI == 6);
                    });
                }

                vehicle.setDecoyReloadCoolDown(400);
                vehicle.setDecoyReady(false);
            }
            vehicle.setDecoyInputDown(false);
        }
        if (!vehicle.getDecoyReady() && vehicle.getDecoyReloadCoolDown() == 0 && vehicle.level() instanceof ServerLevel) {
            vehicle.setDecoyReady(true);
            vehicle.level().playSound(null, vehicle, ModSounds.DECOY_RELOAD.get(), vehicle.getSoundSource(), 1, 1);
            vehicle.setDecoyReloadCoolDown(400);
        }
    }

    public static void shootDecoy(VehicleEntity vehicle, Vec3 shootVec, boolean first) {
        FlareDecoyEntity flareDecoyEntity = new FlareDecoyEntity(vehicle.level());

        flareDecoyEntity.setPos(vehicle.getX() + vehicle.getDeltaMovement().x, vehicle.getY() + 0.5 + vehicle.getDeltaMovement().y, vehicle.getZ() + vehicle.getDeltaMovement().z);
        flareDecoyEntity.decoyShoot(vehicle, shootVec, (float) (vehicle.getDeltaMovement().length() * 0.3f + 0.7), 8);

        vehicle.level().addFreshEntity(flareDecoyEntity);
        vehicle.level().playSound(null, vehicle, first ? ModSounds.DECOY_RELEASE_FIRST.get() : ModSounds.DECOY_RELEASE.get(), vehicle.getSoundSource(), 2, 1);
    }
}
