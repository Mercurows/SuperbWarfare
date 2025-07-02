package com.atsuishio.superbwarfare.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(Dist.CLIENT)
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

        return transformedPos.z < 1.0f ? new Vec3(screenPos.x, screenPos.y, transformedPos.z) : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            fov = event.getFOV();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureCamera(ViewportEvent.ComputeCameraAngles event) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.ZP.rotationDegrees(event.getRoll()));
        poseStack.mulPose(Axis.XP.rotationDegrees(event.getPitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(event.getYaw() + 180.0F));

        cachedPoseStack = poseStack;
    }
}
