package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import java.util.*
import kotlin.random.Random

object SpritePixelHelper {
    private val RANDOM = Random

    /**
     * 从 TextureAtlasSprite 中随机抽取一个像素，返回 RGB 颜色值
     * 
     * @param sprite 纹理精灵
     * @param frame  帧索引（动画纹理时使用，通常为 0）
     * @return RGB 颜色值，格式为 0xRRGGBB
     */
    fun getRandomPixelRGB(sprite: TextureAtlasSprite, frame: Int): Int {
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

    fun getDogTagIcon(list: List<List<Short>>, path: String): ResourceLocation {
        val newDogTagIcon = createDogTagImage(list)
        val newTextureLoc = loc("${path.lowercase(Locale.ROOT)}_dog_tag.png")

        mc.textureManager.register(
            newTextureLoc,
            DynamicTexture(newDogTagIcon)
        )

        return newTextureLoc
    }

    private fun createDogTagImage(icon: List<List<Short>>): NativeImage {
        val dogTag = NativeImage(16, 16, false)
        icon.forEachIndexed { x, shorts ->
            if (x >= 16) return@forEachIndexed
            shorts.forEachIndexed { y, s ->
                if (y >= 16) return@forEachIndexed
                if (s.toInt() == -1) {
                    dogTag.setPixelRGBA(x, y, 0x00000000)
                } else {
                    val color = DogTagEditorScreen.getColorByNum(s)
                    dogTag.setPixelRGBA(x, y, argbToAbgr(color))
                }
            }
        }
        return dogTag
    }

    fun argbToAbgr(argb: Int): Int {
        val a = (argb shr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (a shl 24) or (b shl 16) or (g shl 8) or r
    }
}