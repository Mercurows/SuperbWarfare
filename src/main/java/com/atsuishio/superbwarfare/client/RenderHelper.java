package com.atsuishio.superbwarfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderHelper {

    private static long GUI_RENDER_TIMESTAMP = -1L;

    public static void markGuiRenderTimestamp() {
        GUI_RENDER_TIMESTAMP = System.currentTimeMillis();
    }

    public static boolean isInGui() {
        return System.currentTimeMillis() - GUI_RENDER_TIMESTAMP < 100L;
    }

    public static void preciseBlit(GuiGraphics gui, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight) {
        preciseBlit(gui, pAtlasLocation, pX, pY, 0, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }

    public static void preciseBlit(GuiGraphics gui, ResourceLocation pAtlasLocation, float pX, float pY, float pBlitOffset, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight) {
        float pX2 = pX + pWidth;
        float pY2 = pY + pHeight;

        float pMinU = pUOffset / pTextureWidth;
        float pMaxU = (pUOffset + pWidth) / pTextureWidth;
        float pMinV = pVOffset / pTextureHeight;
        float pMaxV = (pVOffset + pHeight) / pTextureHeight;

        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, pX, pY, pBlitOffset).uv(pMinU, pMinV).endVertex();
        bufferbuilder.vertex(matrix4f, pX, pY2, pBlitOffset).uv(pMinU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).uv(pMaxU, pMaxV).endVertex();
        bufferbuilder.vertex(matrix4f, pX2, pY, pBlitOffset).uv(pMaxU, pMinV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }


    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight, int color) {
        blit(pose, pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight, color);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX, float pY, float pWidth, float pHeight, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight, int color) {
        blit(pose, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, color);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pUWidth, float pVHeight, float pUOffset, float pVOffset, float pTextureWidth, float pTextureHeight, int color) {
        innerBlit(pose, pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0F) / pTextureWidth, (pUOffset + pUWidth) / pTextureWidth, (pVOffset + 0F) / pTextureHeight, (pVOffset + pVHeight) / pTextureHeight, color);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight, float alpha, boolean opposite) {
        blit(pose, pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight, alpha, opposite);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX, float pY, float pUOffset, float pVOffset, float pWidth, float pHeight, float pTextureWidth, float pTextureHeight, float alpha) {
        blit(pose, pAtlasLocation, pX, pY, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight, alpha, false);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX, float pY, float pWidth, float pHeight, float pUOffset, float pVOffset, float pUWidth, float pVHeight, float pTextureWidth, float pTextureHeight, float alpha, boolean opposite) {
        blit(pose, pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, alpha, opposite);
    }

    public static void blit(PoseStack pose, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pUWidth, float pVHeight, float pUOffset, float pVOffset, float pTextureWidth, float pTextureHeight, float alpha, boolean opposite) {
        innerBlit(pose, pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0F) / pTextureWidth, (pUOffset + pUWidth) / pTextureWidth, (pVOffset + 0F) / pTextureHeight, (pVOffset + pVHeight) / pTextureHeight, alpha, opposite);
    }

    private static void innerBlit(PoseStack pose, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, int color) {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        vertexC(pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV, color, matrix4f, bufferbuilder);

        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    private static void innerBlit(PoseStack pose, ResourceLocation pAtlasLocation, float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, float alpha, boolean opposite) {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        if (opposite) {
            vertex(pX1, pX2, pY1, pY2, pBlitOffset, pMaxU, pMinU, pMinV, pMaxV, alpha, matrix4f, bufferbuilder);
        } else {
            vertex(pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV, alpha, matrix4f, bufferbuilder);
        }

        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
    }

    private static void vertex(float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, float alpha, Matrix4f matrix4f, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).color(1f, 1f, 1f, alpha).uv(pMinU, pMinV).endVertex();
        bufferBuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).color(1f, 1f, 1f, alpha).uv(pMinU, pMaxV).endVertex();
        bufferBuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).color(1f, 1f, 1f, alpha).uv(pMaxU, pMaxV).endVertex();
        bufferBuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).color(1f, 1f, 1f, alpha).uv(pMaxU, pMinV).endVertex();
    }

    private static void vertexC(float pX1, float pX2, float pY1, float pY2, float pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, int color, Matrix4f matrix4f, BufferBuilder bufferBuilder) {
        float r = (color >> 16 & 255) / 255F;
        float g = (color >> 8 & 255) / 255F;
        float b = (color & 255) / 255F;

        bufferBuilder.vertex(matrix4f, pX1, pY1, pBlitOffset).color(r, g, b, 1f).uv(pMinU, pMinV).endVertex();
        bufferBuilder.vertex(matrix4f, pX1, pY2, pBlitOffset).color(r, g, b, 1f).uv(pMinU, pMaxV).endVertex();
        bufferBuilder.vertex(matrix4f, pX2, pY2, pBlitOffset).color(r, g, b, 1f).uv(pMaxU, pMaxV).endVertex();
        bufferBuilder.vertex(matrix4f, pX2, pY1, pBlitOffset).color(r, g, b, 1f).uv(pMaxU, pMinV).endVertex();
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the
     * boundaries.
     *
     * @param pRenderType the render type to use.
     * @param pMinX       the minimum x-coordinate of the rectangle.
     * @param pMinY       the minimum y-coordinate of the rectangle.
     * @param pMaxX       the maximum x-coordinate of the rectangle.
     * @param pMaxY       the maximum y-coordinate of the rectangle.
     * @param pZ          the z-level of the rectangle.
     * @param pColor      the color to fill the rectangle with.
     */
    public static void fill(GuiGraphics guiGraphics, RenderType pRenderType, float pMinX, float pMinY, float pMaxX, float pMaxY, float pZ, int pColor) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float) FastColor.ARGB32.alpha(pColor) / 255F;
        float f = (float) FastColor.ARGB32.red(pColor) / 255F;
        float f1 = (float) FastColor.ARGB32.green(pColor) / 255F;
        float f2 = (float) FastColor.ARGB32.blue(pColor) / 255F;
        VertexConsumer vertexconsumer = guiGraphics.bufferSource().getBuffer(pRenderType);
        vertexconsumer.vertex(matrix4f, pMinX, pMinY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMinX, pMaxY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMaxX, pMaxY, pZ).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, pMaxX, pMinY, pZ).color(f, f1, f2, f3).endVertex();
        guiGraphics.flush();
    }

    public static void renderScrollingString(GuiGraphics pGuiGraphics, Font pFont, Component pText, float scale, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        int width = pFont.width(pText);
        int borderWidth = pMaxX - pMinX;
        if (width > borderWidth) {
            int l = width - borderWidth;
            double rate = (double) Util.getMillis() / 1000;
            double d1 = Math.max((double) l * 0.5, 3);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * rate / d1)) / 2 + 0.5;
            double d3 = Mth.lerp(d2, 0, l);
            pGuiGraphics.enableScissor((int) (pMinX * scale), (int) (pMinY * scale), (int) (pMaxX * scale), (int) (pMaxY * scale));
            pGuiGraphics.drawString(pFont, pText, pMinX - (int) d3, pMinY, pColor);
            pGuiGraphics.disableScissor();
        } else {
            pGuiGraphics.drawString(pFont, pText, pMinX, pMinY, pColor);
        }
    }

    /**
     * 渲染一个圆环
     *
     * @param guiGraphics     gui
     * @param centerX         渲染中心X坐标
     * @param centerY         渲染中心Y坐标
     * @param outerRadius     外环半径
     * @param innerRadius     内环半径
     * @param backgroundColor 背景颜色
     * @param progressColor   进度颜色
     * @param progress        进度
     * @param useRate         是否使用占据屏幕百分比形式的半径
     */
    public static void renderCircularRing(GuiGraphics guiGraphics, float centerX, float centerY, float outerRadius, float innerRadius, float[] backgroundColor, float[] progressColor, float progress, boolean useRate) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);

        poseStack.rotateAround(Axis.ZP.rotationDegrees(-90), centerX, centerY, 0);

        var window = Minecraft.getInstance().getWindow();
        float scale = useRate ? Math.min(window.getGuiScaledWidth(), window.getGuiScaledHeight()) : 1;

        // 绘制背景圆环
        drawCircularRing(poseStack, centerX, centerY, outerRadius * scale, innerRadius * scale, backgroundColor, 1.0f);

        // 绘制进度圆环
        drawCircularRing(poseStack, centerX, centerY, outerRadius * scale, innerRadius * scale, progressColor, progress);

        poseStack.popPose();

        RenderSystem.disableBlend();
    }

    public static void drawCircularRing(PoseStack poseStack, float centerX, float centerY, float outerRadius, float innerRadius,
                                        float[] color, float progressAngle) {
        poseStack.pushPose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        Matrix4f matrix = poseStack.last().pose();
        float angleStep = (float) (2 * Math.PI / 180);
        float maxAngle = (float) (2 * Math.PI * progressAngle);

        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

        for (int i = 0; i <= 180 * progressAngle; i++) {
            float angle = i * angleStep;
            if (angle > maxAngle) {
                angle = maxAngle;
            }

            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // 外圆点
            float outerX = centerX + outerRadius * cos;
            float outerY = centerY + outerRadius * sin;
            buffer.vertex(matrix, outerX, outerY, 0).endVertex();

            // 内圆点
            float innerX = centerX + innerRadius * cos;
            float innerY = centerY + innerRadius * sin;
            buffer.vertex(matrix, innerX, innerY, 0).endVertex();

            if (angle >= maxAngle) break;
        }

        tesselator.end();

        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }
}
