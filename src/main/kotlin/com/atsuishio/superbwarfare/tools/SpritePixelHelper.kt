package com.atsuishio.superbwarfare.tools

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import kotlin.random.Random

object SpritePixelHelper {
    private val RANDOM = Random

    /**
     * 从 TextureAtlasSprite 中随机抽取一个像素，返回 ARGB 颜色值
     * 
     * @param sprite 纹理精灵
     * @param frame  帧索引（动画纹理时使用，通常为 0）
     * @return ARGB 颜色值，格式为 0xAARRGGBB
     */
    fun getRandomPixelARGB(sprite: TextureAtlasSprite, frame: Int): Int {
        // 获取纹理尺寸
        val width = sprite.contents().width()
        val height = sprite.contents().height()

        // 生成随机坐标
        val x = RANDOM.nextInt(width)
        val y = RANDOM.nextInt(height)

        // 获取像素值
        val colors = sprite.getPixelRGBA(frame, x, y)

        // 提取分量
        val blue = (colors shr 16) and 0xFF
        val green = (colors shr 8) and 0xFF
        val red = colors and 0xFF

        // 组合为 RGB
        return (red shl 16) or (green shl 8) or blue
    }
}