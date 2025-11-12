package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * 处理载具相关动量、向量和旋转等数据的工具类
 */
public final class VehicleVecUtils {

    public static Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public static double getYRotFromVector(Vec3 vec3) {
        return Mth.atan2(vec3.x, vec3.z) * (180F / Math.PI);
    }

    public static double getXRotFromVector(Vec3 vec3) {
        double d0 = vec3.horizontalDistance();
        return Mth.atan2(vec3.y, d0) * (180F / Math.PI);
    }

    public static double getSubmergedHeight(Entity entity) {
        return entity.getFluidTypeHeight(entity.level().getFluidState(entity.blockPosition()).getFluidType());
    }

    public static Quaternionf eulerToQuaternion(float yaw, float pitch, float roll) {
        double cy = Math.cos(yaw * 0.5 * Mth.DEG_TO_RAD);
        double sy = Math.sin(yaw * 0.5 * Mth.DEG_TO_RAD);
        double cp = Math.cos(pitch * 0.5 * Mth.DEG_TO_RAD);
        double sp = Math.sin(pitch * 0.5 * Mth.DEG_TO_RAD);
        double cr = Math.cos(roll * 0.5 * Mth.DEG_TO_RAD);
        double sr = Math.sin(roll * 0.5 * Mth.DEG_TO_RAD);

        Quaternionf q = new Quaternionf();
        q.w = (float) (cy * cp * cr + sy * sp * sr);
        q.x = (float) (cy * cp * sr - sy * sp * cr);
        q.y = (float) (sy * cp * sr + cy * sp * cr);
        q.z = (float) (sy * cp * cr - cy * sp * sr);

        return q;
    }

    public static double calculateAngle(Vec3 move, Vec3 view) {
        move = move.multiply(1, 0, 1).normalize();
        view = view.multiply(1, 0, 1).normalize();

        return VectorTool.calculateAngle(move, view);
    }

    public static Vec3 entityEyePos(Entity entity, float partialTicks) {
        return new Vec3(Mth.lerp(partialTicks, entity.xo, entity.getX()), Mth.lerp(partialTicks, entity.yo + entity.getEyeHeight(), entity.getEyeY()), Mth.lerp(partialTicks, entity.zo, entity.getZ()));
    }

    public static Vec3 simulate3P(Entity entity, float partialTicks, double distance, double height) {
        return new Vec3(Mth.lerp(partialTicks, entity.xo, entity.getX()) - distance * entity.getViewVector(partialTicks).x,
                Mth.lerp(partialTicks, entity.yo + entity.getEyeHeight() + height, entity.getEyeY() + height) - distance * entity.getViewVector(partialTicks).y,
                Mth.lerp(partialTicks, entity.zo, entity.getZ()) - distance * entity.getViewVector(partialTicks).z);
    }

    /**
     * 将有炮塔的载具驾驶员的面朝方向设置为炮塔角度
     *
     * @param player 载具驾驶员
     */
    public static void setDriverAngle(VehicleEntity vehicle, Player player) {
        if (vehicle.hasTurret()) {
            var barrelVector = vehicle.getBarrelVector(1);

            double xRot = getXRotFromVector(barrelVector);
            double yRot = getYRotFromVector(barrelVector);

            player.xRotO = (float) -xRot;
            player.setXRot((float) -xRot);
            player.yRotO = (float) -yRot;
            player.setYRot((float) -yRot);
            player.setYHeadRot((float) -yRot);
        } else {
            player.xRotO = vehicle.getXRot();
            player.setXRot(vehicle.getXRot());
            player.yRotO = vehicle.getYRot();
            player.setYRot(vehicle.getYRot());
        }
    }

    /**
     * 计算载具受伤来源的角度
     *
     * @param vehicle    载具
     * @param source     伤害来源
     * @param multiplier 伤害倍率
     * @return 角度
     */
    public static float getDamageSourceAngle(VehicleEntity vehicle, DamageSource source, float multiplier) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) {
            attacker = source.getEntity();
        }

        if (attacker != null) {
            Vec3 toVec = new Vec3(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight() / 2, vehicle.getZ()).vectorTo(attacker.position()).normalize();
            return (float) Math.max(1f - multiplier * toVec.dot(vehicle.getViewVector(1)), 0.5f);
        }
        return 1;
    }

    public static void setPassengerPitch(VehicleEntity vehicle, Entity entity, float minPitch, float maxPitch, float passengerRot) {
        if (passengerRot == 180) {
            float min = minPitch + vehicle.getXRot();
            float max = maxPitch + vehicle.getXRot();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        } else {
            float a = -passengerRot;
            float r = (Mth.abs(a) - 90f) / 90f;

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            float min = -maxPitch - r * vehicle.getXRot() - r2 * vehicle.getRoll();
            float max = -minPitch - r * vehicle.getXRot() - r2 * vehicle.getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        }
    }

    public static void setPassengerYaw(VehicleEntity vehicle, Entity entity, float minYaw, float maxYaw, float passengerRot) {
        if (passengerRot == 180) {
            float f2 = Mth.wrapDegrees(entity.getYRot() - vehicle.getYRot() + passengerRot);
            float f3 = Mth.clamp(f2, minYaw, maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        } else {
            float f2 = Mth.wrapDegrees(entity.getYRot() - vehicle.getYRot());
            float f3 = Mth.clamp(f2, passengerRot + minYaw, passengerRot + maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }
        entity.setYBodyRot(vehicle.getYRot() + passengerRot);
    }

    public static void setPassengerPitchOnTurret(VehicleEntity vehicle, Entity entity, float turretMinPitch, float turretMaxPitch) {
        float a = vehicle.getTurretYaw(1);
        float r = (Mth.abs(a) - 90f) / 90f;

        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float min = -turretMaxPitch - r * vehicle.getXRot() - r2 * vehicle.getRoll();
        float max = -turretMinPitch - r * vehicle.getXRot() - r2 * vehicle.getRoll();

        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, min, max);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
    }

    public static void setPassengerYawOnTurret(VehicleEntity vehicle, Entity entity, float minYaw, float maxYaw, float passengerRot, boolean rotateWithTurret) {
        float f2;
        if (passengerRot != 180) {
            f2 = Mth.wrapDegrees(entity.getYRot() - vehicle.getYRot());
            float f3 = Mth.clamp(f2, passengerRot + minYaw, passengerRot + maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        } else {
            f2 = Mth.wrapDegrees(entity.getYRot() - vehicle.getYRot() + 180);
            float f3 = Mth.clamp(f2, minYaw, maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }

        if (rotateWithTurret) {
            entity.setYBodyRot(vehicle.getBarrelYRot(1) + passengerRot);
        }

        clampZoomYaw(vehicle, entity);
    }

    public static void clampZoomYaw(VehicleEntity vehicle, Entity entity) {
        if (!entity.level().isClientSide) return;
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) return;

        if (vehicle.getTurretControllerIndex() == vehicle.getSeatIndex(entity)) {
            float f2 = Mth.wrapDegrees(entity.getYRot() - vehicle.getBarrelYRot(1));
            float f3 = Mth.clamp(f2, -20F, 20F);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }
    }

    /**
     * 获取载具视角向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 视角向量
     */
    public static Vec3 getViewVec(VehicleEntity vehicle, Entity entity, float partialTicks) {
        var data = vehicle.getGunData(vehicle.getSeatIndex(entity));
        if (data == null) {
            return vehicle.getViewVector(partialTicks);
        }

        StringOrVec3 stringOrVec3 = data.compute().shootPos.viewDirection;

        if (stringOrVec3 == null) {
            return vehicle.getShootVec(entity, partialTicks);
        } else if (stringOrVec3.isString()) {
            return vehicle.getVectorFromString(stringOrVec3.string, partialTicks, vehicle.getSeatIndex(entity));
        } else {
            var vec3 = stringOrVec3.vec3;
            Vector4f worldPosition = transformPosition(
                    vehicle.getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x + (float) stringOrVec3.vec3.x,
                    (float) vec3.y + (float) stringOrVec3.vec3.y,
                    (float) vec3.z + (float) stringOrVec3.vec3.z);

            Vector4f worldPositionO = transformPosition(
                    vehicle.getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x,
                    (float) vec3.y,
                    (float) vec3.z);

            Vec3 startPos = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
            Vec3 endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            return startPos.vectorTo(endPos).normalize();
        }
    }

    /**
     * 获取载具射击向量
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 射击向量
     */
    public static Vec3 getShootVec(VehicleEntity vehicle, Entity entity, float partialTicks) {
        var data = vehicle.getGunData(vehicle.getSeatIndex(entity));
        if (data == null) {
            return vehicle.getViewVector(partialTicks);
        }

        var stringOrVec3 = data.fireDirection();

        if (stringOrVec3.isString()) {
            return vehicle.getVectorFromString(stringOrVec3.string, partialTicks, vehicle.getSeatIndex(entity));
        } else {
            var vec3 = data.firePosition();

            var worldPosition = transformPosition(
                    vehicle.getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x + (float) stringOrVec3.vec3.x,
                    (float) vec3.y + (float) stringOrVec3.vec3.y,
                    (float) vec3.z + (float) stringOrVec3.vec3.z);

            var worldPositionO = transformPosition(
                    vehicle.getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x,
                    (float) vec3.y,
                    (float) vec3.z);

            var startPos = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
            var endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            return startPos.vectorTo(endPos).normalize();
        }
    }

    /**
     * 获取乘客在载具上的摄像机位置
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 摄像机位置
     */
    public static Vec3 getCameraPos(VehicleEntity vehicle, Entity entity, float partialTicks) {
        int index = vehicle.getSeatIndex(entity);
        var seat = vehicle.computed().seats().get(index);
        if (seat == null) {
            return entityEyePos(entity, partialTicks);
        }

        var data = seat.cameraPos;
        if (data == null) {
            return entityEyePos(entity, partialTicks);
        }

        if (data.useSimulate3P) {
            var simulate3PPos = data.simulate3PPos;
            return simulate3P(entity, partialTicks, simulate3PPos.x, simulate3PPos.y);
        }
        if (data.useFixedCameraPos) {
            var vec3 = data.position;
            Vector4f worldPosition = transformPosition(vehicle.getTransformFromString(data.transform, partialTicks), (float) vec3.x, (float) vec3.y, (float) vec3.z);
            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }

        return entityEyePos(entity, partialTicks);
    }

    /**
     * 获取乘客在载具上的摄像机方向
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 摄像机方向
     */
    public static Vec3 getCameraDirection(VehicleEntity vehicle, Entity entity, float partialTicks) {
        int index = vehicle.getSeatIndex(entity);
        var seat = vehicle.computed().seats().get(index);
        if (seat == null) {
            return entity.getViewVector(partialTicks);
        }

        var data = seat.cameraPos;

        if (data == null) {
            return entity.getViewVector(partialTicks);
        }

        if (data.useSimulate3P) {
            return entity.getViewVector(partialTicks);
        }

        StringOrVec3 stringOrVec3 = data.direction;
        if (stringOrVec3.isString()) {
            if (stringOrVec3.string.equals("Default")) {
                if (getZoomDirection(vehicle, entity, partialTicks) != null && ClientEventHandler.zoomVehicle) {
                    return vehicle.getZoomDirection(entity, partialTicks);
                } else {
                    return entity.getViewVector(partialTicks);
                }
            } else {
                return vehicle.getVectorFromString(stringOrVec3.string, partialTicks, vehicle.getSeatIndex(entity));
            }
        } else {
            var vec3 = data.position;

            Vector4f worldPosition = transformPosition(
                    vehicle.getTransformFromString(data.transform, partialTicks),
                    (float) vec3.x + (float) stringOrVec3.vec3.x,
                    (float) vec3.y + (float) stringOrVec3.vec3.y,
                    (float) vec3.z + (float) stringOrVec3.vec3.z);

            Vec3 startPos = getCameraPos(vehicle, entity, partialTicks);
            Vec3 endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            return startPos.vectorTo(endPos).normalize();
        }
    }

    /**
     * 获取载具瞄准的坐标
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 瞄准坐标
     */
    public static Vec3 getZoomPos(VehicleEntity vehicle, Entity entity, float partialTicks) {
        int index = vehicle.getSeatIndex(entity);
        var seat = vehicle.computed().seats().get(index);
        if (seat == null) {
            return VehicleVecUtils.entityEyePos(entity, partialTicks);
        }

        var data = seat.cameraPos;
        if (data == null) {
            return VehicleVecUtils.entityEyePos(entity, partialTicks);
        }

        var vec3 = data.zoomPosition;
        if (vec3 != null) {
            Vector4f worldPosition = transformPosition(
                    vehicle.getTransformFromString(data.transform, partialTicks), (float) vec3.x, (float) vec3.y, (float) vec3.z
            );
            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        } else {
            return VehicleVecUtils.getCameraPos(vehicle, entity, partialTicks);
        }
    }

    /**
     * 获取载具瞄准的方向
     *
     * @param vehicle      载具
     * @param entity       乘客
     * @param partialTicks 客户端ticks
     * @return 瞄准方向
     */
    public static Vec3 getZoomDirection(VehicleEntity vehicle, Entity entity, float partialTicks) {
        int index = vehicle.getSeatIndex(entity);
        var seat = vehicle.computed().seats().get(index);
        if (seat == null) {
            return entity.getViewVector(partialTicks);
        }

        var data = seat.cameraPos;
        if (data == null) {
            return entity.getViewVector(partialTicks);
        }

        StringOrVec3 stringOrVec3 = data.zoomDirection;
        if (stringOrVec3 != null) {
            if (stringOrVec3.isString()) {
                return vehicle.getVectorFromString(stringOrVec3.string, partialTicks, vehicle.getSeatIndex(entity));
            } else {
                var vec3 = data.zoomPosition;
                Vector4f worldPosition = transformPosition(
                        vehicle.getTransformFromString(data.transform, partialTicks),
                        (float) vec3.x + (float) stringOrVec3.vec3.x,
                        (float) vec3.y + (float) stringOrVec3.vec3.y,
                        (float) vec3.z + (float) stringOrVec3.vec3.z);

                Vec3 startPos = vehicle.getShootPos(entity, partialTicks);
                Vec3 endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
                return startPos.vectorTo(endPos).normalize();
            }
        } else {
            return vehicle.cameraDirection(entity, partialTicks);
        }
    }

    // From Immersive_Aircraft
    public static Matrix4f getVehicleYOffsetTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = new Matrix4f();
        transform.translate(
                (float) Mth.lerp(partialTicks, vehicle.xo, vehicle.getX()),
                (float) Mth.lerp(partialTicks, vehicle.yo + vehicle.rotateOffsetHeight(), vehicle.getY() + vehicle.rotateOffsetHeight()),
                (float) Mth.lerp(partialTicks, vehicle.zo, vehicle.getZ())
        );
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, vehicle.xRotO, vehicle.getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.getRoll())));
        return transform;
    }

    public static Matrix4f getVehicleFlatTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = new Matrix4f();
        transform.translate(
                (float) Mth.lerp(partialTicks, vehicle.xo, vehicle.getX()),
                (float) Mth.lerp(partialTicks, vehicle.yo, vehicle.getY()),
                (float) Mth.lerp(partialTicks, vehicle.zo, vehicle.getZ())
        );
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.getYRot())));
        return transform;
    }

    public static Matrix4f getClientVehicleTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = new Matrix4f();
        transform.translate(
                (float) Mth.lerp(partialTicks, vehicle.xo, vehicle.getX()),
                (float) Mth.lerp(partialTicks, vehicle.yo + vehicle.rotateOffsetHeight(), vehicle.getY() + vehicle.rotateOffsetHeight()),
                (float) Mth.lerp(partialTicks, vehicle.zo, vehicle.getZ())
        );
        transform.rotate(Axis.YP.rotationDegrees((float) (-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.getYRot()) + ClientMouseHandler.freeCameraYaw)));
        transform.rotate(Axis.XP.rotationDegrees((float) (Mth.lerp(partialTicks, vehicle.xRotO, vehicle.getXRot()) + ClientMouseHandler.freeCameraPitch)));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.getRoll())));
        return transform;
    }

    /**
     * 获取炮塔的旋转矩阵
     *
     * @param vehicle      载具
     * @param partialTicks 客户端ticks
     * @return 旋转矩阵
     */
    public static Matrix4f getTurretTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transformV = vehicle.getVehicleTransform(partialTicks);

        Matrix4f transform = new Matrix4f();
        var pos = vehicle.getTurretPos();
        if (pos == null) return transformV;
        Vector4f worldPosition = transformPosition(
                transform,
                (float) pos.x,
                (float) pos.y,
                (float) pos.z
        );

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, vehicle.turretYRotO, vehicle.getTurretYRot())));
        return transformV;
    }

    /**
     * 获取炮塔的向量
     *
     * @param vehicle      载具
     * @param partialTicks 客户端ticks
     * @return 炮塔向量
     */
    public static Vec3 getTurretVector(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = getTurretTransform(vehicle, partialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public static Matrix4f getBarrelTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transformT = getTurretTransform(vehicle, partialTicks);

        Matrix4f transform = new Matrix4f();
        var pos = vehicle.getBarrelPosition();
        Vector4f worldPosition = transformPosition(
                transform,
                (float) pos.x,
                (float) pos.y,
                (float) pos.z
        );

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        float a = vehicle.getTurretYaw(partialTicks);
        float r = (Mth.abs(a) - 90f) / 90f;
        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float x = Mth.lerp(partialTicks, vehicle.turretXRotO, vehicle.getTurretXRot());
        float xV = Mth.lerp(partialTicks, vehicle.xRotO, vehicle.getXRot());
        float z = Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.getRoll());

        transformT.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformT;
    }

    public static Matrix4f getGunTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transformT = getTurretTransform(vehicle, partialTicks);

        Matrix4f transform = new Matrix4f();
        var pos = vehicle.getPassengerWeaponStationPosition();
        Vector4f worldPosition = transformPosition(
                transform,
                (float) pos.x,
                (float) pos.y,
                (float) pos.z
        );

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformT.rotate(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, vehicle.gunYRotO, vehicle.getGunYRot()) - Mth.lerp(partialTicks, vehicle.turretYRotO, vehicle.getTurretYRot())));
        return transformT;
    }

    public static Matrix4f getPassengerWeaponStationBarrelTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transformG = getGunTransform(vehicle, partialTicks);

        Matrix4f transform = new Matrix4f();
        var pos = vehicle.getPassengerWeaponStationBarrelPosition();
        Vector4f worldPosition = transformPosition(
                transform,
                (float) pos.x,
                (float) pos.y,
                (float) pos.z
        );

        transformG.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        float a = vehicle.getTurretYaw(partialTicks);
        float r = (Mth.abs(a) - 90f) / 90f;
        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float x = Mth.lerp(partialTicks, vehicle.gunXRotO, vehicle.getGunXRot());
        float xV = Mth.lerp(partialTicks, vehicle.xRotO, vehicle.getXRot());
        float z = Mth.lerp(partialTicks, vehicle.prevRoll, vehicle.getRoll());

        transformG.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformG;
    }

    public static Vec3 getPassengerWeaponStationVector(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = getPassengerWeaponStationBarrelTransform(vehicle, partialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }
}
