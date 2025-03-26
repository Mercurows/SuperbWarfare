package com.atsuishio.superbwarfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class RenderHelper {
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
                (uOffset + 0.0F) / textureWidth,
                (uOffset + uWidth) / textureWidth,
                (vOffset + 0.0F) / textureHeight,
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
     * @param red           the red component of the color tfloat.
     * @param green         the green component of the color tfloat.
     * @param blue          the blue component of the color tfloat.
     * @param alpha         the alpha component of the color tfloat.
     */
    public static void innerBlit(
            GuiGraphics gui,
            ResourceLocation atlasLocation,
            float x1, float x2,
            float y1, float y2,
            float blitOffset,
            float minU, float maxU,
            float minV, float maxV,
            float red, float green, float blue, float alpha
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = gui.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, x1, y1, blitOffset)
                .setUv(minU, minV)
                .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, x1, y2, blitOffset)
                .setUv(minU, maxV)
                .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, x2, y2, blitOffset)
                .setUv(maxU, maxV)
                .setColor(red, green, blue, alpha);
        bufferbuilder.addVertex(matrix4f, x2, y1, blitOffset)
                .setUv(maxU, minV)
                .setColor(red, green, blue, alpha);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }
}
