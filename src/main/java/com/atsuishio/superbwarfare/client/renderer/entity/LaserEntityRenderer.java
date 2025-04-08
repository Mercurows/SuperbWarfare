package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.LaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Code based on @BobMowzie's MowziesMobs and @EEEAB's EEEABsMobs
 */
@OnlyIn(Dist.CLIENT)
public class LaserEntityRenderer extends AbstractLaserEntityRenderer<LaserEntity> {

    private boolean playerView;

    private static final float TEXTURE_WIDTH = 256;
    private static final float TEXTURE_HEIGHT = 32;

    public static final ResourceLocation TEXTURE = Mod.loc("textures/entity/temp_laser.png");

    public LaserEntityRenderer(EntityRendererProvider.Context context) {
        super(context, 0.8f, 0.6f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LaserEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(LaserEntity beam, float entityYaw, float delta, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        this.playerView = beam.caster instanceof Player && Minecraft.getInstance().player == beam.caster
                && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
//        if (this.playerView) return;
        super.render(beam, entityYaw, delta, matrixStackIn, bufferIn, packedLightIn);
    }

    @Override
    protected void renderFlatQuad(int frame, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn, boolean inGround) {
        float minU = 0 + 16F / TEXTURE_WIDTH * frame;
        float minV = 0;
        float maxU = minU + 16F / TEXTURE_WIDTH;
        float maxV = minV + 16F / TEXTURE_HEIGHT;
        float size = 0.25f;
        PoseStack.Pose pose = matrixStackIn.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        drawVertex(matrix4f, matrix3f, pose, builder, -size, -size, 0, minU, minV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, -size, size, 0, minU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, size, 0, maxU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, -size, 0, maxU, minV, packedLightIn);
    }

    @Override
    protected void renderStart(LaserEntity laser, int frame, PoseStack matrixStackIn, VertexConsumer builder, float delta, int packedLightIn) {
        if (this.playerView) return;
        super.renderStart(laser, frame, matrixStackIn, builder, delta, packedLightIn);
    }

    @Override
    protected void drawBeam(float length, int frame, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn) {
        float minU = 0;
        float minV = 16 / TEXTURE_HEIGHT + 1 / TEXTURE_HEIGHT * frame;
        float maxU = minU + 20 / TEXTURE_WIDTH;
        float maxV = minV + 1 / TEXTURE_HEIGHT;
        PoseStack.Pose pose = matrixStackIn.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        float offset = playerView ? -1 : 0;
        float size = 0.2f;
        drawVertex(matrix4f, matrix3f, pose, builder, -size, offset, 0, minU, minV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, -size, length, 0, minU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, length, 0, maxU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, offset, 0, maxU, minV, packedLightIn);
    }
}
