package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
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
import org.joml.Math;
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

    @Shadow
    @Deprecated
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V", ordinal = 0),
            method = "setup",
            cancellable = true)
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo info) {
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

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
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
                && player.getMainHandItem().is(ModTags.Items.GUN)
                && Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos) > 0
        ) {
            move(-getMaxZoom((float) (-2.9 * Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos))), 0F, (float) (-ClientEventHandler.cameraLocation * Math.max(ClientEventHandler.bowPullPos, ClientEventHandler.zoomPos)));
            return;
        }

        if (!thirdPerson || !(entity.getVehicle() instanceof VehicleEntity vehicle)) return;

        var cameraPosition = vehicle.getThirdPersonCameraPosition(vehicle.getSeatIndex(entity));
        if (cameraPosition != null) {
            move(-getMaxZoom((float) cameraPosition.distance()), (float) cameraPosition.y(), -(float) cameraPosition.z());
        }
    }

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract float getMaxZoom(float maxZoom);
}