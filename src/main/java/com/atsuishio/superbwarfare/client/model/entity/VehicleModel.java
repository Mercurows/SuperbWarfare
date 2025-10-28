package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public abstract class VehicleModel<T extends VehicleEntity & GeoAnimatable> extends GeoModel<T> {

    protected float pitch;
    protected float yaw;
    protected float roll;
    protected float leftWheelRot;
    protected float rightWheelRot;
    protected float turretYRot;
    protected float turretXRot;
    protected float turretYaw;
    protected float recoilShake;
    protected boolean hideFor1stPassengerWhileZooming;

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return null;
    }


    @Override
    public ResourceLocation getTextureResource(T entity) {
        return null;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        float partialTick = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        pitch = animatable.getPitch(partialTick);
        yaw = animatable.getYaw(partialTick);
        roll = animatable.getRoll(partialTick);

        leftWheelRot = Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot());
        rightWheelRot = Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot());

        turretYRot = Mth.lerp(partialTick, animatable.turretYRotO, animatable.getTurretYRot());
        turretXRot = Mth.lerp(partialTick, animatable.turretXRotO, animatable.getTurretXRot());

        turretYaw = animatable.getTurretYaw(partialTick);

        recoilShake = Mth.lerp(partialTick, (float) animatable.recoilShakeO, (float) animatable.getRecoilShake());

        hideFor1stPassengerWhileZooming = ClientEventHandler.zoomVehicle && animatable.getFirstPassenger() == Minecraft.getInstance().player;

        // turret.*

        var turret = getAnimationProcessor().getBone("turret");

        if (turret != null) {
            turret.setHidden(hideFor1stPassengerWhileZooming);
            turret.setRotY(turretYRot * Mth.DEG_TO_RAD);
        }

        // flare.*
        var flare = getAnimationProcessor().getBone("flare");

        if (flare != null) {
            flare.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }

        // barrel.*

        var barrel = getAnimationProcessor().getBone("barrel");

        if (barrel != null) {
            float a = turretYaw;
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

            barrel.setRotX(-turretXRot * Mth.DEG_TO_RAD - r * pitch * Mth.DEG_TO_RAD - r2 * roll * Mth.DEG_TO_RAD);
        }

        // TODO 怎么根据命名找对应模型

        // track(Mov|Rot)\d+
//        if (hasTrack() && name.length() > 9 && name.startsWith("track")) {
//            var isL = name.charAt(9) == 'L';
//
//            if (name.startsWith("trackRot")) {
//                int i = Integer.parseInt(name.substring(9));
//                if (isL) {
//                    float t = wrap(vehicle.getLeftTrack() + 2 * i);
//                    float tO = wrap(vehicle.leftTrackO + 2 * i);
//                    bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO), getBoneRotX(t)) * Mth.DEG_TO_RAD);
//                } else {
//                    float tO2 = wrap(vehicle.rightTrackO + 2 * i);
//                    float t2 = wrap(vehicle.getRightTrack() + 2 * i);
//                    bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO2), getBoneRotX(t2)) * Mth.DEG_TO_RAD);
//                }
//            } else if (name.startsWith("trackMov")) {
//                int i = Integer.parseInt(name.substring(9));
//                if (isL) {
//                    float tO = wrap(vehicle.leftTrackO + 2 * i);
//                    float t = wrap(vehicle.getLeftTrack() + 2 * i);
//                    bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO), getBoneMoveY(t)));
//                    bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO), getBoneMoveZ(t)));
//                } else {
//                    float tO2 = wrap(vehicle.rightTrackO + 2 * i);
//                    float t2 = wrap(vehicle.getRightTrack() + 2 * i);
//                    bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO2), getBoneMoveY(t2)));
//                    bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO2), getBoneMoveZ(t2)));
//                }
//            }
//        }
    }

    public boolean hasTrack() {
        return false;
    }

    public float getBoneRotX(float t) {
        return t;
    }

    public float getBoneMoveY(float t) {
        return t;
    }

    public float getBoneMoveZ(float t) {
        return t;
    }

    protected float wrap(float value, int range) {
        return ((value % range) + range) % range;
    }

    protected float wrap(float value) {
        return wrap(value, getDefaultWrapRange());
    }

    public int getDefaultWrapRange() {
        return 100;
    }
}
