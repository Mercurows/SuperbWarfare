package com.atsuishio.superbwarfare.client.renderer

import com.mojang.blaze3d.platform.GlStateManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

/** Depth blits require matching storage formats, including Iris's floating-point/stencil targets. */
internal fun matchDepthFormat(source: Int, target: Int) {
    GlStateManager._bindTexture(source)
    val format = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT)
    GlStateManager._bindTexture(target)
    if (GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT) == format) return
    val width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH)
    val height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)
    val stencil = format == GL30.GL_DEPTH24_STENCIL8 || format == GL30.GL_DEPTH32F_STENCIL8
    val type = when (format) {
        GL30.GL_DEPTH32F_STENCIL8 -> GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV
        GL30.GL_DEPTH24_STENCIL8 -> GL30.GL_UNSIGNED_INT_24_8
        GL30.GL_DEPTH_COMPONENT32F -> GL11.GL_FLOAT
        else -> GL11.GL_UNSIGNED_INT
    }
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0,
        if (stencil) GL30.GL_DEPTH_STENCIL else GL11.GL_DEPTH_COMPONENT, type, 0L)
}
