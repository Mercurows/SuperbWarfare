package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class CircularProgressHUD implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("circular");

    // 进度值 (0.0 - 1.0)
    private float progress = 0.0f;

    // 圆环参数
    private int centerX = 40;
    private int centerY = 40;
    private int outerRadius = 30;
    private int innerRadius = 20;
    private int segments = 120; // 分段数量，越多越平滑

    // 颜色
    private final float[] progressColor = {0.2f, 1f, 0.2f, 1.0f}; // RGBA
    private final float[] backgroundColor = {0.3f, 0.3f, 0.3f, 0.3f}; // RGBA


    // TODO 能够在在任意位置调用这个HUD，并设置进度，位置，大小，颜色
    public void setProgress(float progress) {
        this.progress = Mth.clamp(progress, 0, 1);
    }

    public float getProgress() {
        return progress;
    }

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);

        poseStack.rotateAround(Axis.ZP.rotationDegrees(-90), centerX, centerY, 0);

        // 绘制背景圆环
        drawCircularRing(poseStack, centerX, centerY, outerRadius, innerRadius, backgroundColor, 1.0f);

        // 绘制进度圆环
        drawCircularRing(poseStack, centerX, centerY, outerRadius, innerRadius, progressColor, progress);

        poseStack.popPose();

        RenderSystem.disableBlend();

    }

    private void drawCircularRing(PoseStack poseStack, int centerX, int centerY, int outerRadius, int innerRadius,
                                  float[] color, float progressAngle) {
        Tesselator tessellator = Tesselator.getInstance();

        Matrix4f matrix = poseStack.last().pose();
        float angleStep = (float) (2 * Math.PI / segments);
        float maxAngle = (float) (2 * Math.PI * progressAngle);

        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);

        BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

        for (int i = 0; i <= segments * progressAngle; i++) {
            float angle = i * angleStep;
            if (angle > maxAngle) {
                angle = maxAngle;
            }

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // 外圆点
            float outerX = centerX + outerRadius * cos;
            float outerY = centerY + outerRadius * sin;
            buffer.addVertex(matrix, outerX, outerY, 0);

            // 内圆点
            float innerX = centerX + innerRadius * cos;
            float innerY = centerY + innerRadius * sin;
            buffer.addVertex(matrix, innerX, innerY, 0);

            if (angle >= maxAngle) break;
        }

        tessellator.clear();

        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}