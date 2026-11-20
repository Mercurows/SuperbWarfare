package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.camera.VehicleBoneCamera;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomTime;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow @Final private static Vector3f FORWARDS;
    @Shadow @Final private static Vector3f UP;
    @Shadow @Final private static Vector3f LEFT;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private float roll;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V", ordinal = 0),
            method = "setup",
            cancellable = true)
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo info) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || entity != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) return;

        boolean zoom = ClientEventHandler.zoomVehicle;
        if (zoom || !detached) {
            var view = VehicleBoneCamera.resolve(vehicle, vehicle.getSeatIndex(player), partialTicks, zoom);
            if (view != null) {
                setPosition(view.getPosition());
                superbWarfare$setBoneOrientation(view.getOrientation());
                VehicleBoneCamera.markActive();
                info.cancel();
                return;
            }
        }

        var vehicleRotation = vehicle.getCameraRotation(partialTicks, player, zoom, !detached);
        if (vehicleRotation != null) {
            setRotation(vehicleRotation.x, vehicleRotation.y, 0.0f);
        }
        var position = vehicle.getCameraPosition(partialTicks, player, zoom, !detached);
        if (position != null) {
            setPosition(position);
        }
        if (vehicleRotation != null || position != null) {
            info.cancel();
        }
    }

    private void superbWarfare$setBoneOrientation(Quaternionf orientation) {
        this.rotation.set(orientation);
        FORWARDS.rotate(orientation, this.forwards);
        UP.rotate(orientation, this.up);
        LEFT.rotate(orientation, this.left);

        // 保留依赖角度的接口，视图矩阵直接使用四元数。
        this.yRot = (float) Math.toDegrees(Math.atan2(-this.forwards.x, this.forwards.z));
        this.xRot = (float) Math.toDegrees(-Math.asin(Mth.clamp(this.forwards.y, -1.0f, 1.0f)));
        this.roll = (float) Math.toDegrees(Math.atan2(this.left.y, this.up.y));
    }

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && entity instanceof Player player
                && GunItem.isHeldWeapon(player.getMainHandItem())
                && zoomTime > 0
        ) {
            double zoom = ClientEventHandler.aimingProgress(zoomTime);
            move(-getMaxZoom((float) (-2.9 * zoom)), 0f, (float) (-ClientEventHandler.cameraLocation * zoom));
            return;
        }

        if (!thirdPerson || !(entity.getVehicle() instanceof VehicleEntity vehicle)) return;

        var cameraPosition = vehicle.getThirdPersonCameraPosition();
        move(-getMaxZoom((float) cameraPosition.x()), (float) cameraPosition.y(), (float) cameraPosition.z());
    }

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract float getMaxZoom(float maxZoom);
}
