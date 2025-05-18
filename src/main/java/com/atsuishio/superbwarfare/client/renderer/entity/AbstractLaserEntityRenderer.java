package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.atsuishio.superbwarfare.entity.projectile.AbstractLaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Code based on @BobMowzie's MowziesMobs and @EEEAB's EEEABsMobs
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractLaserEntityRenderer<T extends AbstractLaserEntity> extends EntityRenderer<T> {

    private static final float TEXTURE_WIDTH = 256;
    private static final float TEXTURE_HEIGHT = 32;
    private final float quadRadius;
    private final float beamRadius;

    public AbstractLaserEntityRenderer(EntityRendererProvider.Context context, float quadRadius, float beamRadius) {
        super(context);
        this.quadRadius = quadRadius;
        this.beamRadius = beamRadius;
    }

    @Override
    public abstract @NotNull ResourceLocation getTextureLocation(@NotNull T entity);

    @Override
    public void render(T beam, float entityYaw, float delta, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        double collidePosX = beam.prevCollidePosX + (beam.collidePosX - beam.prevCollidePosX) * delta;
        double collidePosY = beam.prevCollidePosY + (beam.collidePosY - beam.prevCollidePosY) * delta;
        double collidePosZ = beam.prevCollidePosZ + (beam.collidePosZ - beam.prevCollidePosZ) * delta;
        double posX = beam.xo + (beam.getX() - beam.xo) * delta;
        double posY = beam.yo + (beam.getY() - beam.yo) * delta;
        double posZ = beam.zo + (beam.getZ() - beam.zo) * delta;
        float yaw = beam.preYaw + (beam.yaw - beam.preYaw) * delta;
        float pitch = beam.prePitch + (beam.pitch - beam.prePitch) * delta;

        float length = (float) Math.sqrt(Math.pow(collidePosX - posX, 2) + Math.pow(collidePosY - posY, 2) + Math.pow(collidePosZ - posZ, 2));
        int frame = Mth.floor((beam.ticker.getTick() - 1 + delta) * 2);
        if (frame < 0) {
            frame = 6;
        }

        if (!beam.isAccumulating()) return;
        VertexConsumer vertex$builder = bufferIn.getBuffer(ModRenderTypes.LASER.apply(getTextureLocation(beam)));
        renderStart(beam, frame, matrixStackIn, vertex$builder, delta, packedLightIn);
        renderBeam(length, 180f / (float) Math.PI * yaw, 180f / (float) Math.PI * pitch, frame, matrixStackIn, vertex$builder, packedLightIn);
        matrixStackIn.pushPose();
        matrixStackIn.translate(collidePosX - posX, collidePosY - posY, collidePosZ - posZ);
        renderEnd(beam, frame, beam.blockSide, matrixStackIn, vertex$builder, delta, packedLightIn);
        matrixStackIn.popPose();
    }

    protected void renderFlatQuad(int frame, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn, boolean inGround) {
        float minU = 0 + 16F / TEXTURE_WIDTH * frame;
        float minV = 0;
        float maxU = minU + 16F / TEXTURE_WIDTH;
        float maxV = minV + 16F / TEXTURE_HEIGHT;
        float SIZE = this.quadRadius + (inGround ? 0.2F : 0);
        PoseStack.Pose pose = matrixStackIn.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        drawVertex(matrix4f, matrix3f, pose, builder, -SIZE, -SIZE, 0, minU, minV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, -SIZE, SIZE, 0, minU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, SIZE, SIZE, 0, maxU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, SIZE, -SIZE, 0, maxU, minV, packedLightIn);
    }

    protected void renderStart(T entity, int frame, PoseStack matrixStackIn, VertexConsumer builder, float delta, int packedLightIn) {
        matrixStackIn.pushPose();
        Quaternionf quaternionf = this.entityRenderDispatcher.cameraOrientation();
        matrixStackIn.mulPose(quaternionf);
        renderFlatQuad(frame, matrixStackIn, builder, packedLightIn, false);
        matrixStackIn.popPose();
    }

    protected void renderEnd(T entity, int frame, Direction side, PoseStack matrixStackIn, VertexConsumer builder, float delta, int packedLightIn) {
        matrixStackIn.pushPose();
        Quaternionf quaternionf = this.entityRenderDispatcher.cameraOrientation();
        matrixStackIn.mulPose(quaternionf);
        renderFlatQuad(frame, matrixStackIn, builder, packedLightIn, false);
        matrixStackIn.popPose();

        if (side == null) {
            return;
        }

        matrixStackIn.pushPose();
        Quaternionf rotation = side.getRotation();
        rotation.mul(Axis.XP.rotationDegrees(90F));
        matrixStackIn.mulPose(rotation);
        matrixStackIn.translate(0, 0, -0.01f);
        renderFlatQuad(frame, matrixStackIn, builder, packedLightIn, true);
        matrixStackIn.popPose();
    }

    protected void renderBeam(float length, float yaw, float pitch, int frame, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn) {
        matrixStackIn.pushPose();

        matrixStackIn.mulPose(Axis.XP.rotationDegrees(90F));
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(yaw - 90F));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(-pitch));

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() + 90F));
        drawBeam(length, frame, matrixStackIn, builder, packedLightIn);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() - 90F));
        drawBeam(length, frame, matrixStackIn, builder, packedLightIn);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() + 180F));
        drawBeam(length, frame, matrixStackIn, builder, packedLightIn);
        matrixStackIn.popPose();

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getXRot() - 180F));
        drawBeam(length, frame, matrixStackIn, builder, packedLightIn);
        matrixStackIn.popPose();

        matrixStackIn.popPose();
    }

    protected void drawBeam(float length, int frame, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn) {
        float minU = 0;
        float minV = 16 / TEXTURE_HEIGHT + 1 / TEXTURE_HEIGHT * frame;
        float maxU = minU + 20 / TEXTURE_WIDTH;
        float maxV = minV + 1 / TEXTURE_HEIGHT;
        PoseStack.Pose pose = matrixStackIn.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        float offset = 0;
        float size = this.beamRadius;
        drawVertex(matrix4f, matrix3f, pose, builder, -size, offset, 0, minU, minV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, -size, length, 0, minU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, length, 0, maxU, maxV, packedLightIn);
        drawVertex(matrix4f, matrix3f, pose, builder, size, offset, 0, maxU, minV, packedLightIn);
    }

    protected void drawVertex(Matrix4f matrix, Matrix3f normals, PoseStack.Pose pose, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, int packedLightIn) {
        vertexBuilder.addVertex(matrix, offsetX, offsetY, offsetZ)
                .setColor(1F, 1F, 1F, 1F).setUv(textureX, textureY)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLightIn)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

}
