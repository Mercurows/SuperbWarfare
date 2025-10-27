package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public abstract class VehicleRenderer<T extends VehicleEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    public VehicleRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public RenderType getRenderType(T vehicle, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(vehicle));
    }

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
    public void preRender(PoseStack poseStack, T vehicle, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        pitch = vehicle.getPitch(partialTick);
        yaw = vehicle.getYaw(partialTick);
        roll = vehicle.getRoll(partialTick);

        leftWheelRot = Mth.lerp(partialTick, vehicle.leftWheelRotO, vehicle.getLeftWheelRot());
        rightWheelRot = Mth.lerp(partialTick, vehicle.rightWheelRotO, vehicle.getRightWheelRot());

        turretYRot = Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.getTurretYRot());
        turretXRot = Mth.lerp(partialTick, vehicle.turretXRotO, vehicle.getTurretXRot());

        turretYaw = vehicle.getTurretYaw(partialTick);

        recoilShake = Mth.lerp(partialTick, (float) vehicle.recoilShakeO, (float) vehicle.getRecoilShake());

        hideFor1stPassengerWhileZooming = ClientEventHandler.zoomVehicle && vehicle.getFirstPassenger() == Minecraft.getInstance().player;

        super.preRender(poseStack, vehicle, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    protected void processBone(PoseStack poseStack, T vehicle, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        var name = bone.getName();

        // TODO 正确旋转轮子
        // wheel[LR].*
        if (hasTrackWheel() && name.length() >= 6 && name.startsWith("wheel")) {
            char LR = name.charAt(5);
            if (LR == 'L') {
                bone.setRotX(1.5f * leftWheelRot);
            } else if (LR == 'R') {
                bone.setRotX(1.5f * rightWheelRot);
            }
        }

        // flare.*
        if (name.startsWith("flare")) {
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }

        // barrel.*
        if (hasBarrel() && name.startsWith("barrel")) {
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

            bone.setRotX(-turretXRot * Mth.DEG_TO_RAD - r * pitch * Mth.DEG_TO_RAD - r2 * roll * Mth.DEG_TO_RAD);
        }

        // track(Mov|Rot)\d+
        if (hasTrack() && name.length() > 9 && name.startsWith("track")) {
            var isL = name.charAt(9) == 'L';

            if (name.startsWith("trackRot")) {
                int i = Integer.parseInt(name.substring(9));
                if (isL) {
                    float t = wrap(vehicle.getLeftTrack() + 2 * i);
                    float tO = wrap(vehicle.leftTrackO + 2 * i);
                    bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO), getBoneRotX(t)) * Mth.DEG_TO_RAD);
                } else {
                    float tO2 = wrap(vehicle.rightTrackO + 2 * i);
                    float t2 = wrap(vehicle.getRightTrack() + 2 * i);
                    bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO2), getBoneRotX(t2)) * Mth.DEG_TO_RAD);
                }
            } else if (name.startsWith("trackMov")) {
                int i = Integer.parseInt(name.substring(9));
                if (isL) {
                    float tO = wrap(vehicle.leftTrackO + 2 * i);
                    float t = wrap(vehicle.getLeftTrack() + 2 * i);
                    bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO), getBoneMoveY(t)));
                    bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO), getBoneMoveZ(t)));
                } else {
                    float tO2 = wrap(vehicle.rightTrackO + 2 * i);
                    float t2 = wrap(vehicle.getRightTrack() + 2 * i);
                    bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO2), getBoneMoveY(t2)));
                    bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO2), getBoneMoveZ(t2)));
                }
            }
        }
    }

    public boolean hasTrack() {
        return false;
    }

    public boolean hasTrackWheel() {
        return hasTrack();
    }

    public boolean hasBarrel() {
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
