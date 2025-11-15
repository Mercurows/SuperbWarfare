package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.ICustomCamera;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import org.joml.Math;
import org.joml.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements ICustomCamera {

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow(aliases = "Lnet/minecraft/client/Camera;setRotation(FF)V")
    protected abstract void setRotation(float p_90573_, float p_90574_);

    @Shadow(aliases = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    protected abstract void setPosition(double p_90585_, double p_90586_, double p_90587_);

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            cancellable = true)
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo info) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null) {
            ItemStack stack = player.getMainHandItem();

            if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
                DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
                if (drone != null) {
                    boolean firstPerson = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK;
                    if (firstPerson) {
                        Matrix4f transform = superbWarfare$getDroneTransform(drone, partialTicks);
                        float x0 = 0f;
                        float y0 = 0.075f;
                        float z0 = 0.18f;

                        Vector4f worldPosition = superbWarfare$transformPosition(transform, x0, y0, z0);

                        setRotation(drone.getYaw(partialTicks), drone.getPitch(partialTicks));
                        setPosition(worldPosition.x, worldPosition.y, worldPosition.z);
                        info.cancel();
                    } else {
                        var rotation = drone.getCameraRotation(partialTicks, player, false, false);
                        if (rotation != null) {
                            setRotation(rotation.x, rotation.y);
                        }
                        var position = drone.getCameraPosition(partialTicks, player, false, false);
                        if (position != null) {
                            setPosition(position.x, position.y, position.z);
                        }

                        if (rotation != null || position != null) {
                            info.cancel();
                        }
                    }
                }
                return;
            }

            if (player.getVehicle() instanceof VehicleEntity vehicle) {
//                // TODO 完善四元数相关
//                var quat = vehicle.getCameraQuat(partialTicks, player, ClientEventHandler.zoomVehicle, Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
//                if (quat != null) {
//                    this.rotation.set(quat);
//
//                    this.xRot = quat.x;
//                    this.yRot = quat.y;
//
//                    this.forwards.set(0, 0, 1).rotate(this.rotation);
//                    this.up.set(0, 1, 0).rotate(this.rotation);
//                    this.left.set(1, 0, 0).rotate(this.rotation);
//
//                    info.cancel();
//                }

                var rotation = vehicle.getCameraRotation(partialTicks, player, ClientEventHandler.zoomVehicle, Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
                if (rotation != null) {
                    setRotation(rotation.x, rotation.y);
                }
                var position = vehicle.getCameraPosition(partialTicks, player, ClientEventHandler.zoomVehicle, Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
                if (position != null) {
                    setPosition(position.x, position.y, position.z);
                }

                if (rotation != null || position != null) {
                    info.cancel();
                }

            }
        }
    }

    @Unique
    private static Matrix4f superbWarfare$getDroneTransform(DroneEntity vehicle, float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, vehicle.xo, vehicle.getX()), (float) Mth.lerp(ticks, vehicle.yo, vehicle.getY()), (float) Mth.lerp(ticks, vehicle.zo, vehicle.getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-vehicle.getYaw(ticks)));
        transform.rotate(Axis.XP.rotationDegrees(vehicle.getBodyPitch(ticks)));
        transform.rotate(Axis.ZP.rotationDegrees(vehicle.getRoll(ticks)));
        return transform;
    }

    @Unique
    private static Vector4f superbWarfare$transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && entity instanceof Player player
                && player.getMainHandItem().getItem() instanceof GunItem
                && Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos) > 0
        ) {
            move(-getMaxZoom(-2.9 * Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos)), 0, -ClientEventHandler.cameraLocation * Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos));
            return;
        }

        if (!thirdPerson || !(entity.getVehicle() instanceof VehicleEntity vehicle)) return;

        var cameraPosition = vehicle.getThirdPersonCameraPosition();
        if (cameraPosition != null) {
            move(-getMaxZoom(cameraPosition.distance()), cameraPosition.y(), cameraPosition.z());
        }
    }

    @Shadow
    protected abstract void move(double x, double y, double z);

    @Shadow
    protected abstract double getMaxZoom(double desiredCameraDistance);

    @Shadow @Final private Vector3f forwards;

    @Shadow @Final private Vector3f up;

    @Shadow @Final private Vector3f left;

    @Shadow private float xRot;

    @Shadow private float yRot;

    @Override
    public Quaternionf superbwarfare$getRotation() {
        return this.rotation;
    }
}