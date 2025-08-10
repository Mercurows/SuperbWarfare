package com.atsuishio.superbwarfare.tools;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

    public static boolean canSee(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());
        Vec3 v1 = cameraPos.vectorTo(pos);
        return VectorTool.calculateAngle(v1, viewVec) < fov - 10;
    }
}
