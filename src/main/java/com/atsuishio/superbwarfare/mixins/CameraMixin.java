package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.*;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow(aliases = "Lnet/minecraft/client/Camera;setRotation(FF)V")
    protected abstract void setRotation(float x, float y);

    @Shadow(aliases = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    protected abstract void setPosition(double x, double y, double z);

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            cancellable = true)
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo info) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
            if (drone != null) {
                Matrix4f transform = superbWarfare$getDroneTransform(drone, partialTicks);
                float x0 = 0f;
                float y0 = 0.075f;
                float z0 = 0.18f;

                Vector4f worldPosition = superbWarfare$transformPosition(transform, x0, y0, z0);

                setRotation(drone.getYaw(partialTicks), drone.getPitch(partialTicks));
                setPosition(worldPosition.x, worldPosition.y, worldPosition.z);
                info.cancel();
            }
            return;
        }

        if ((player.getVehicle() != null && player.getVehicle() instanceof SpeedboatEntity boat && boat.getFirstPassenger() == player) && ClientEventHandler.zoomVehicle) {
            setRotation((float) -VehicleEntity.getYRotFromVector(boat.getBarrelVec(partialTicks)), (float) -VehicleEntity.getXRotFromVector(boat.getBarrelVec(partialTicks)));
            if (ClientEventHandler.zoomVehicle) {
                setPosition(boat.driverZoomPos(partialTicks).x, boat.driverZoomPos(partialTicks).y, boat.driverZoomPos(partialTicks).z);
            } else {
                setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            }
            info.cancel();
            return;
        }

        if (player.getVehicle() instanceof Lav150Entity lav150 && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) {
            if (lav150.getFirstPassenger() == player) {
                setRotation((float) -VehicleEntity.getYRotFromVector(lav150.getBarrelVec(partialTicks)), (float) -VehicleEntity.getXRotFromVector(lav150.getBarrelVec(partialTicks)));
                if (ClientEventHandler.zoomVehicle) {
                    setPosition(lav150.driverZoomPos(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), lav150.driverZoomPos(partialTicks).z);
                } else {
                    setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
                info.cancel();
            } else {
                setRotation(Mth.lerp(partialTicks, player.yHeadRotO, player.getYHeadRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
                setPosition(Mth.lerp(partialTicks, player.xo, player.getX()) - 6 * player.getViewVector(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight() + 1, player.getEyeY() + 1) - 6 * player.getViewVector(partialTicks).y, Mth.lerp(partialTicks, player.zo, player.getZ()) - 6 * player.getViewVector(partialTicks).z);
                info.cancel();
            }
            return;
        }

        if (player.getVehicle() instanceof Bmp2Entity bmp2 && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) {
            if (bmp2.getFirstPassenger() == player) {
                setRotation((float) -VehicleEntity.getYRotFromVector(bmp2.getBarrelVec(partialTicks)), (float) -VehicleEntity.getXRotFromVector(bmp2.getBarrelVec(partialTicks)));
                if (ClientEventHandler.zoomVehicle) {
                    setPosition(bmp2.driverZoomPos(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), bmp2.driverZoomPos(partialTicks).z);
                } else {
                    setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
                info.cancel();
            } else {
                setRotation(Mth.lerp(partialTicks, player.yHeadRotO, player.getYHeadRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
                setPosition(Mth.lerp(partialTicks, player.xo, player.getX()) - 6 * player.getViewVector(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight() + 1, player.getEyeY() + 1) - 6 * player.getViewVector(partialTicks).y, Mth.lerp(partialTicks, player.zo, player.getZ()) - 6 * player.getViewVector(partialTicks).z);
                info.cancel();
            }
            return;
        }

        if (player.getVehicle() instanceof Yx100Entity yx100 && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) {
            if (yx100.getFirstPassenger() == player) {
                setRotation((float) -VehicleEntity.getYRotFromVector(yx100.getBarrelVec(partialTicks)), (float) -VehicleEntity.getXRotFromVector(yx100.getBarrelVec(partialTicks)));
                if (ClientEventHandler.zoomVehicle) {
                    setPosition(yx100.driverZoomPos(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), yx100.driverZoomPos(partialTicks).z);
                } else {
                    setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
                info.cancel();
            } else if (yx100.getNthEntity(1) == player) {
                setRotation((float) -VehicleEntity.getYRotFromVector(yx100.getGunnerVector(partialTicks)), (float) -VehicleEntity.getXRotFromVector(yx100.getGunnerVector(partialTicks)));
                setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                info.cancel();
            }
            return;
        }

        if (player.getVehicle() instanceof CannonEntity && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle)) {
            setRotation(Mth.lerp(partialTicks, player.yRotO, player.getYRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
            setPosition(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            info.cancel();
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

    // TODO camera mixin, maybe use CalculateDetachedCameraDistanceEvent instead?
    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && entity instanceof Player player
                && player.getMainHandItem().is(ModTags.Items.GUN)
                && Math.max(ClientEventHandler.pullPos, ClientEventHandler.zoomPos) > 0
        ) {
            move(-getMaxZoom((float) (-2.9 * Math.max(ClientEventHandler.pullPos, ClientEventHandler.zoomPos))), 0, (float) (-ClientEventHandler.cameraLocation * Math.max(ClientEventHandler.pullPos, ClientEventHandler.zoomPos)));
            return;
        }

        if (!thirdPerson || !(entity.getVehicle() instanceof VehicleEntity vehicle)) return;

        var cameraPosition = vehicle.getThirdPersonCameraPosition(vehicle.getSeatIndex(entity));
        if (cameraPosition != null) {
            move(-getMaxZoom((float) cameraPosition.distance()), (float) cameraPosition.y(), (float) cameraPosition.z());
        }
    }

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract float getMaxZoom(float maxZoom);
}