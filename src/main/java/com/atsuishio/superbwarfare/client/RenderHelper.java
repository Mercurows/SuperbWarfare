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

    // code from GuiGraphics

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the blit position.
     * @param y             the y-coordinate of the blit position.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     */
    public static void preciseBlit(GuiGraphics gui, ResourceLocation atlasLocation, float x, float y, float uOffset, float vOffset, float uWidth, float vHeight) {
        preciseBlit(gui, atlasLocation, x, y, 0, uOffset, vOffset, uWidth, vHeight, 256, 256);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates with a blit offset and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the blit position.
     * @param y             the y-coordinate of the blit position.
     * @param blitOffset    the z-level offset for rendering order.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public static void preciseBlit(
            GuiGraphics gui, ResourceLocation atlasLocation,
            float x, float y,
            float blitOffset,
            float uOffset, float vOffset,
            float uWidth, float vHeight,
            float textureWidth, float textureHeight
    ) {
        preciseBlit(
                gui, atlasLocation,
                x, x + uWidth,
                y, y + vHeight,
                blitOffset,
                uWidth, vHeight,
                uOffset, vOffset,
                textureWidth, textureHeight
        );
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the top-left corner of the blit
     *                      position.
     * @param y             the y-coordinate of the top-left corner of the blit
     *                      position.
     * @param width         the width of the blitted portion.
     * @param height        the height of the blitted portion.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public static void preciseBlit(
            GuiGraphics gui, ResourceLocation atlasLocation,
            float x, float y,
            float width, float height,
            float uOffset, float vOffset,
            float uWidth, float vHeight,
            float textureWidth, float textureHeight
    ) {
        preciseBlit(
                gui, atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight
        );
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the top-left corner of the blit
     *                      position.
     * @param y             the y-coordinate of the top-left corner of the blit
     *                      position.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param width         the width of the blitted portion.
     * @param height        the height of the blitted portion.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public static void preciseBlit(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x, float y,
            float uOffset, float vOffset,
            float width, float height,
            float textureWidth, float textureHeight
    ) {
        preciseBlit(gui, atlasLocation, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public static void preciseBlitWithColor(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x, float y,
            float uOffset, float vOffset,
            float width, float height,
            float textureWidth, float textureHeight,
            int color
    ) {
        preciseBlitWithColor(gui, atlasLocation, x, y, uOffset, vOffset, 0, width, height, textureWidth, textureHeight, color);
    }

    public static void preciseBlitWithColor(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x, float y,
            float uOffset, float vOffset,
            float blitOffset,
            float width, float height,
            float textureWidth, float textureHeight,
            int color
    ) {
        innerBlit(
                gui, atlasLocation,
                x, x + width,
                y, y + height,
                blitOffset,
                uOffset / textureWidth,
                (uOffset + width) / textureWidth,
                vOffset / textureHeight,
                (vOffset + height) / textureHeight,
                color | 0xFF000000
        );
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position
     *                      .
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position
     *                      .
     * @param blitOffset    the z-level offset for rendering order.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public static void preciseBlit(
            GuiGraphics gui, ResourceLocation atlasLocation,
            float x1, float x2,
            float y1, float y2,
            float blitOffset,
            float uWidth, float vHeight,
            float uOffset, float vOffset,
            float textureWidth, float textureHeight
    ) {
        innerBlit(
                gui, atlasLocation,
                x1, x2,
                y1, y2,
                blitOffset,
                uOffset / textureWidth,
                (uOffset + uWidth) / textureWidth,
                vOffset / textureHeight,
                (vOffset + vHeight) / textureHeight
        );
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates without color tfloating.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position
     *                      .
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position
     *                      .
     * @param blitOffset    the z-level offset for rendering order.
     * @param minU          the minimum horizontal texture coordinate.
     * @param maxU          the maximum horizontal texture coordinate.
     * @param minV          the minimum vertical texture coordinate.
     * @param maxV          the maximum vertical texture coordinate.
     */
    public static void innerBlit(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x1, float x2,
            float y1, float y2,
            float blitOffset,
            float minU, float maxU,
            float minV, float maxV
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, x1, y1, blitOffset).setUv(minU, minV);
        bufferbuilder.addVertex(matrix4f, x1, y2, blitOffset).setUv(minU, maxV);
        bufferbuilder.addVertex(matrix4f, x2, y2, blitOffset).setUv(maxU, maxV);
        bufferbuilder.addVertex(matrix4f, x2, y1, blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates, texture coordinates, and color tfloat.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position
     *                      .
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position
     *                      .
     * @param blitOffset    the z-level offset for rendering order.
     * @param minU          the minimum horizontal texture coordinate.
     * @param maxU          the maximum horizontal texture coordinate.
     * @param minV          the minimum vertical texture coordinate.
     * @param maxV          the maximum vertical texture coordinate.
     * @param color         color
     */
    public static void innerBlit(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x1, float x2,
            float y1, float y2,
            float blitOffset,
            float minU, float maxU,
            float minV, float maxV,
            int color
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, x1, y1, blitOffset)
                .setUv(minU, minV)
                .setColor(color);
        bufferbuilder.addVertex(matrix4f, x1, y2, blitOffset)
                .setUv(minU, maxV)
                .setColor(color);
        bufferbuilder.addVertex(matrix4f, x2, y2, blitOffset)
                .setUv(maxU, maxV)
                .setColor(color);
        bufferbuilder.addVertex(matrix4f, x2, y1, blitOffset)
                .setUv(maxU, minV)
                .setColor(color);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }


    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the boundaries.
     *
     * @param renderType the render type to use.
     * @param minX       the minimum x-coordinate of the rectangle.
     * @param minY       the minimum y-coordinate of the rectangle.
     * @param maxX       the maximum x-coordinate of the rectangle.
     * @param maxY       the maximum y-coordinate of the rectangle.
     * @param z          the z-level of the rectangle.
     * @param color      the color to fill the rectangle with.
     */
    public static void fill(GuiGraphics guiGraphics, RenderType renderType, float minX, float minY, float maxX, float maxY, float z, int color) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            float j = minY;
            minY = maxY;
            maxY = j;
        }

        VertexConsumer vertexconsumer = guiGraphics.bufferSource().getBuffer(renderType);
        vertexconsumer.addVertex(matrix4f, minX, minY, z).setColor(color);
        vertexconsumer.addVertex(matrix4f, minX, maxY, z).setColor(color);
        vertexconsumer.addVertex(matrix4f, maxX, maxY, z).setColor(color);
        vertexconsumer.addVertex(matrix4f, maxX, minY, z).setColor(color);

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
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.rotateAround(Axis.ZP.rotationDegrees(-90), centerX, centerY, 0);

        var window = Minecraft.getInstance().getWindow();
        float scale = useRate ? Math.min(window.getGuiScaledWidth(), window.getGuiScaledHeight()) : 1;

        // 绘制背景圆环
        drawCircularRing(guiGraphics, centerX, centerY, outerRadius * scale, innerRadius * scale, backgroundColor, 1.0f);

        // 绘制进度圆环
        drawCircularRing(guiGraphics, centerX, centerY, outerRadius * scale, innerRadius * scale, progressColor, progress);

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    public static void drawCircularRing(GuiGraphics guiGraphics, float centerX, float centerY, float outerRadius, float innerRadius, float[] color, float progressAngle) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        PoseStack.Pose pose = guiGraphics.pose().last();
        Matrix4f matrix = pose.pose();
        float angleStep = (float) (2 * Math.PI / 180);
        float maxAngle = (float) (2 * Math.PI * progressAngle);

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
            buffer.addVertex(matrix, outerX, outerY, 0)
                    .setColor(color[0], color[1], color[2], color[3]);

            // 内圆点
            float innerX = centerX + innerRadius * cos;
            float innerY = centerY + innerRadius * sin;
            buffer.addVertex(matrix, innerX, innerY, 0)
                    .setColor(color[0], color[1], color[2], color[3]);

            if (angle >= maxAngle) break;
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
