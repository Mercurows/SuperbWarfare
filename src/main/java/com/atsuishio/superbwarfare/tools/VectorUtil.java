package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(Dist.CLIENT)
public class VectorUtil {

    public static double fov = 70;

    public static Matrix4f modelViewMatrix;
    public static Matrix4f projectionMatrix;

    // 感谢 Minecraft-Ping-Wheel 开源
    // https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/ede72b18f57bd9dfe55ef44afe61190421fbc084/common/src/main/java/nx/pingwheel/common/helper/MathUtils.java#L15
    public static Vec3 worldToScreen(Vec3 pos) {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        var camera = mc.gameRenderer.getMainCamera();

        var worldPosRel = new Vector4f(camera.getPosition().reverse().add(pos).toVector3f(), 1f);
        worldPosRel.mul(modelViewMatrix);
        worldPosRel.mul(projectionMatrix);

        var depth = worldPosRel.w;

        if (depth != 0) {
            worldPosRel.div(depth);
        }

        return new Vec3(
                window.getGuiScaledWidth() * (0.5f + worldPosRel.x * 0.5f),
                window.getGuiScaledHeight() * (0.5f - worldPosRel.y * 0.5f),
                depth
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            fov = event.getFOV();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureCamera(ViewportEvent.ComputeCameraAngles event) {
        float roll;
        roll = ClientEventHandler.cameraRoll;

        Entity entity = event.getCamera().getEntity();

        if (entity instanceof Player player && !player.isSpectator() && player.hasEffect(ModMobEffects.SHOCK)) {
            float shakeStrength = (float) DisplayConfig.SHOCK_SCREEN_SHAKE.get() / 100.0f;
            if (shakeStrength <= 0.0f) return;
            roll = (float) Mth.nextDouble(RandomSource.create(), 8, 12) * shakeStrength;
        }

        if (entity.getRootVehicle() instanceof VehicleEntity vehicle && (!event.getCamera().isDetached() || vehicle instanceof LandArmorEntity && ClientEventHandler.zoomVehicle)) {
            // rotate camera
            float a = vehicle.getTurretYaw((float) event.getPartialTick());
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

            roll = -r * vehicle.getRoll((float) event.getPartialTick()) + r2 * vehicle.getViewXRot((float) event.getPartialTick());
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(event.getPitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(event.getYaw() + 180.0F));

    }
}
