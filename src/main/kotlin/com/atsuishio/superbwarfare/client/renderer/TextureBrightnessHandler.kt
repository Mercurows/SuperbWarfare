package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import kotlin.math.min

object TextureBrightnessHandler {
    // 缓存处理过的纹理，避免重复处理
    private val BRIGHTENED_TEXTURES: MutableMap<ResourceLocation, ResourceLocation> = hashMapOf()

    fun getBrightenedTexture(originalTextureLoc: ResourceLocation, brightnessMultiplier: Float): ResourceLocation {
        // 检查是否已缓存
        if (BRIGHTENED_TEXTURES.containsKey(originalTextureLoc)) {
            return BRIGHTENED_TEXTURES[originalTextureLoc]!!
        }

        try {
            // 1. 获取原始纹理
            val resourceManager = mc.resourceManager
            val resource = resourceManager.getResource(originalTextureLoc).orElseThrow()

            // 2. 读取图像
            val originalImage = NativeImage.read(resource.open())
            val brightenedImage = brightenImage(originalImage, brightnessMultiplier)

            // 3. 创建新的纹理资源
            val newTextureLoc = ResourceLocation(
                originalTextureLoc.namespace,
                originalTextureLoc.path.replace(".png", "_bright.png")
            )

            // 4. 注册到纹理管理器
            mc.textureManager.register(
                newTextureLoc,
                DynamicTexture(brightenedImage)
            )

            // 5. 缓存并返回
            BRIGHTENED_TEXTURES[originalTextureLoc] = newTextureLoc
            return newTextureLoc
        } catch (e: Exception) {
            // 出错时返回原始纹理
            e.printStackTrace()
            return originalTextureLoc
        }
    }

    fun brightenImage(original: NativeImage, multiplier: Float): NativeImage {
        // 创建相同尺寸的新图像
        val brightened = NativeImage(original.width, original.height, false)

        for (x in 0..<original.width) {
            for (y in 0..<original.height) {
                val color = original.getPixelRGBA(x, y)

                // 提取 ARGB 通道
                val alpha = (color shr 24) and 0xFF
                var red = (color shr 16) and 0xFF
                var green = (color shr 8) and 0xFF
                var blue = color and 0xFF

                // 增加亮度（确保不超过255）
                red = min(255, (red * multiplier).toInt())
                green = min(255, (green * multiplier).toInt())
                blue = min(255, (blue * multiplier).toInt())

                // 重新组合颜色
                val newColor = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                brightened.setPixelRGBA(x, y, newColor)
            }
        }

        return brightened
    }
}