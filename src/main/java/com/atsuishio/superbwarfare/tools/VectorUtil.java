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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class VectorUtil {

    /**
     * Codes based on @Xjqsh
     */
    private static PoseStack cachedPoseStack = new PoseStack();
    public static double fov = 70;

    public static Vec3 worldToScreen(Vec3 pos, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();



        Matrix4f matrix4f = cachedPoseStack.last().pose();
        var projectionMatrix = mc.gameRenderer.getProjectionMatrix(fov);

        Vector3f relativePos = pos.subtract(cameraPos).toVector3f();

        Vector3f transformedPos = projectionMatrix.mul(matrix4f).transformProject(
                relativePos.x,
                relativePos.y,
                relativePos.z,
                new Vector3f()
        );

        double scaleFactor = mc.getWindow().getGuiScale();
        float guiScaleMul = 0.5f / (float) scaleFactor;

        Vector3f screenPos = transformedPos.mul(1.0F, -1.0F, 1.0F).add(1.0F, 1.0F, 0.0F)
                .mul(guiScaleMul * mc.getWindow().getWidth(), guiScaleMul * mc.getWindow().getHeight(), 1.0F);

        return transformedPos.z<1.0f ? new Vec3(screenPos.x, screenPos.y, transformedPos.z) : null;
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

        if (entity instanceof Player player && !player.isSpectator() && player.hasEffect(ModMobEffects.SHOCK.get())) {
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

        cachedPoseStack = poseStack;
    }
}
