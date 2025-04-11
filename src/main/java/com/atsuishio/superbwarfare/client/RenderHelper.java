package com.atsuishio.superbwarfare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

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
     * Codes based on @Xjqsh
     */
    @Nullable
    public static Vec3 worldToScreen(Vec3 pos, Vec3 cameraPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Frustum frustum = minecraft.levelRenderer.getFrustum();

        Vector3f relativePos = pos.subtract(cameraPos).toVector3f();
        Vector3f transformedPos = frustum.matrix.transformProject(relativePos.x, relativePos.y, relativePos.z, new Vector3f());

        double scaleFactor = minecraft.getWindow().getGuiScale();
        float guiScaleMul = (float) (0.5f / scaleFactor);

        Vector3f screenPos = transformedPos.mul(1.0f, -1.0f, 1.0f).add(1.0f, 1.0f, 0.0f)
                .mul(guiScaleMul * minecraft.getWindow().getWidth(), guiScaleMul * minecraft.getWindow().getHeight(), 1.0f);

        return transformedPos.z < 1.0f ? new Vec3(screenPos.x, screenPos.y, transformedPos.z) : null;
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
}
