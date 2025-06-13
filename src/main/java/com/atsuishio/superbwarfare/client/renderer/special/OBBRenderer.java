package com.atsuishio.superbwarfare.client.renderer.special;

import com.atsuishio.superbwarfare.tools.OBB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Codes based on @AnECanSaiTin's <a href="https://github.com/AnECanSaiTin/HitboxAPI">HitboxAPI</a>
 **/
public class OBBRenderer {

    public static final OBBRenderer INSTANCE = new OBBRenderer();

    public void render(Entity entity, OBB obb, PoseStack poseStack, VertexConsumer buffer, float red, float green, float blue, float alpha) {
        Vec3 position = entity.position();
        Vector3f center = obb.center();
        Vector3f halfExtents = obb.extents();
        Quaternionf rotation = obb.rotation();
        renderOBB(
                poseStack, buffer,
                (float) (center.x() - position.x()), (float) (center.y() - position.y()), (float) (center.z() - position.z()),
                rotation,
                halfExtents.x(), halfExtents.y(), halfExtents.z(),
                red, green, blue, alpha
        );
    }

    public static void renderOBB(PoseStack poseStack, VertexConsumer buffer, float centerX, float centerY, float centerZ, Quaternionf rotation, float halfX, float halfY, float halfZ, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(rotation);
        LevelRenderer.renderLineBox(poseStack, buffer, -halfX, -halfY, -halfZ, halfX, halfY, halfZ, red, green, blue, alpha);
        poseStack.popPose();
    }
}
