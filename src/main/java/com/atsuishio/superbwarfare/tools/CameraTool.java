package com.atsuishio.superbwarfare.tools;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector4d;

public class CameraTool {
    public static Vec3 getMaxZoom(Matrix4d transform, Vector4d maxCameraPos) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        Vector4d vehiclePos = transformPosition(transform, 0, 0, 0);
        Vec3 maxCameraPosVec3 = new Vec3(maxCameraPos.x, maxCameraPos.y, maxCameraPos.z);
        Vec3 vehiclePosVec3 = new Vec3(vehiclePos.x, vehiclePos.y, vehiclePos.z);
        Vec3 toVec = vehiclePosVec3.vectorTo(maxCameraPosVec3);

        if (player != null) {
            HitResult hitresult = player.level().clip(new ClipContext(vehiclePosVec3, vehiclePosVec3.add(toVec).add(toVec.normalize().scale(1)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hitresult.getType() == HitResult.Type.BLOCK) {
                return hitresult.getLocation().add(toVec.normalize().scale(-1));
            }
        }
        return maxCameraPosVec3;
    }

    public static Vector4d transformPosition(Matrix4d transform, float x, float y, float z) {
        return transform.transform(new Vector4d(x, y, z, 1));
    }
}
