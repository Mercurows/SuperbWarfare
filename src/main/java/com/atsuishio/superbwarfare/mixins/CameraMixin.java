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
    @Shadow(aliases = "Lnet/minecraft/client/Camera;setRotation(FF)V")
    protected abstract void setRotation(float p_90573_, float p_90574_);

    @Shadow(aliases = "Lnet/minecraft/client/Camera;setPosition(DDD)V")
    protected abstract void setPosition(double p_90585_, double p_90586_, double p_90587_);

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            cancellable = true)
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTicks, CallbackInfo info) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null) {
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                boolean zoom = ClientEventHandler.zoomVehicle;
                boolean firstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;

                if (VehicleBoneCamera.shouldNailCamera(vehicle, player)) {
                    VehicleBoneCamera.RigidView view =
                            VehicleBoneCamera.resolve(vehicle, vehicle.getSeatIndex(player), partialTicks, zoom);
                    if (view != null) {
                        setPosition(view.getPosition().x, view.getPosition().y, view.getPosition().z);
                        applyRigidOrientation(view.getOrientation());
                        VehicleBoneCamera.markActive(view.getOrientation());
                        info.cancel();
                        return;
                    }
                }

                var rotation = vehicle.getCameraRotation(partialTicks, player, zoom, firstPerson);
                if (rotation != null) {
                    setRotation(rotation.x, rotation.y);
                }
                var position = vehicle.getCameraPosition(partialTicks, player, zoom, firstPerson);
                if (position != null) {
                    setPosition(position.x, position.y, position.z);
                }

                if (rotation != null || position != null) {
                    info.cancel();
                }
            }
        }
    }

    private void applyRigidOrientation(Quaternionf orientation) {
        this.rotation.set(orientation);
        this.forwards.set(0.0f, 0.0f, 1.0f).rotate(orientation);
        this.up.set(0.0f, 1.0f, 0.0f).rotate(orientation);
        this.left.set(1.0f, 0.0f, 0.0f).rotate(orientation);

        // 兼容仍然读取 xRot/yRot 的旧代码：只由前向量反推 pitch/yaw（无滚转分量），不参与真实渲染朝向
        float fx = this.forwards.x();
        float fy = this.forwards.y();
        float fz = this.forwards.z();
        this.yRot = (float) Math.toDegrees(Math.atan2(-fx, fz));
        this.xRot = (float) Math.toDegrees(-Math.asin(Mth.clamp(fy, -1.0f, 1.0f)));
    }

    @Inject(method = "setup", at = @At("TAIL"))
    public void superbWarfare$setup(BlockGetter area, Entity entity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && entity instanceof Player player
                && GunItem.isHeldWeapon(player.getMainHandItem())
                && zoomTime > 0
        ) {
            double zoom = ClientEventHandler.aimingProgress(zoomTime);
            move(-getMaxZoom(-2.9 * zoom), 0, -ClientEventHandler.cameraLocation * zoom);
            return;
        }

        if (!thirdPerson || !(entity.getVehicle() instanceof VehicleEntity vehicle)) return;

        var cameraPosition = vehicle.getThirdPersonCameraPosition();
        move(-getMaxZoom(cameraPosition.x()), cameraPosition.y(), cameraPosition.z());
    }

    @Shadow
    protected abstract void move(double x, double y, double z);

    @Shadow
    protected abstract double getMaxZoom(double desiredCameraDistance);
}
