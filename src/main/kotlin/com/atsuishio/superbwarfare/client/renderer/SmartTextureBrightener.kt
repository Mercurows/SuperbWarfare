package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object SmartTextureBrightener {
    private val BRIGHTNESS_CACHE: MutableMap<ResourceLocation, Float> = hashMapOf()
    private val PROCESSED_TEXTURES: MutableMap<ResourceLocation, ResourceLocation> = hashMapOf()

    // 计算图像的感知亮度（使用人类视觉的加权平均）
    fun calculatePerceivedBrightness(image: NativeImage): Float {
        var totalLuminance: Long = 0
        var pixelCount = 0

        for (x in 0..<image.width) {
            for (y in 0..<image.height) {
                val color = image.getPixelRGBA(x, y)
                val alpha = (color shr 24) and 0xFF

                // 只处理不透明或半透明的像素
                if (alpha > 10) {
                    val r = ((color shr 16) and 0xFF) / 255.0f
                    val g = ((color shr 8) and 0xFF) / 255.0f
                    val b = (color and 0xFF) / 255.0f

                    // 计算感知亮度
                    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    totalLuminance += (luminance * 255).toInt().toLong()
                    pixelCount++
                }
            }
        }

        if (pixelCount == 0) return 0.5f
        return (totalLuminance / (pixelCount * 255).toFloat())
    }

    // 计算图像亮度分布（判断是偏暗、正常还是偏亮）
    fun analyzeBrightnessDistribution(image: NativeImage): BrightnessCategory {
        var darkPixels = 0
        var midPixels = 0
        var brightPixels = 0
        var totalPixels = 0

        for (x in 0..<image.width) {
            for (y in 0..<image.height) {
                val color = image.getPixelRGBA(x, y)
                val alpha = (color shr 24) and 0xFF

                if (alpha > 10) {
                    val r = ((color shr 16) and 0xFF) / 255.0f
                    val g = ((color shr 8) and 0xFF) / 255.0f
                    val b = (color and 0xFF) / 255.0f
                    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

                    if (luminance < 0.3f) darkPixels++
                    else if (luminance < 0.7f) midPixels++
                    else brightPixels++

                    totalPixels++
                }
            }
        }

        if (totalPixels == 0) return BrightnessCategory.NORMAL

        val darkRatio = darkPixels / totalPixels.toFloat()
        val brightRatio = brightPixels / totalPixels.toFloat()

        if (darkRatio > 0.6f) return BrightnessCategory.DARK
        if (brightRatio > 0.6f) return BrightnessCategory.BRIGHT
        return BrightnessCategory.NORMAL
    }

    // 根据图像特征动态计算亮度系数
    fun calculateDynamicBrightnessFactor(image: NativeImage): Float {
        val averageBrightness = calculatePerceivedBrightness(image)
        val category = analyzeBrightnessDistribution(image)

        // 基础调整：根据平均亮度
        var baseFactor: Float
        if (averageBrightness < 0.2f) {
            // 很暗的贴图：大幅提亮
            baseFactor = 1.8f + (0.2f - averageBrightness) * 2.0f
        } else if (averageBrightness < 0.4f) {
            // 较暗的贴图：中等提亮
            baseFactor = 1.3f + (0.4f - averageBrightness) * 1.25f
        } else if (averageBrightness < 0.6f) {
            // 正常贴图：轻微提亮
            baseFactor = 1.0f + (0.6f - averageBrightness) * 0.5f
        } else if (averageBrightness < 0.8f) {
            // 较亮贴图：基本保持
            baseFactor = 1.0f - (averageBrightness - 0.6f) * 0.25f
        } else {
            // 很亮贴图：稍微压暗
            baseFactor = 0.85f - (averageBrightness - 0.8f) * 0.5f
        }

        // 根据分布微调
        baseFactor *= when (category) {
            BrightnessCategory.DARK -> 1.1f

            BrightnessCategory.BRIGHT -> 0.9f

            else -> 1.0f
        }
        // 限制在合理范围内
        return max(0.5f, min(3.0f, baseFactor))
    }

    // 智能调整亮度（自适应）
    fun smartBrighten(original: NativeImage, targetBrightness: Float): NativeImage {
        val currentBrightness = calculatePerceivedBrightness(original)
        val dynamicFactor = calculateDynamicBrightnessFactor(original)

        // 如果指定了目标亮度，则基于目标调整
        var factor: Float
        if (targetBrightness > 0) {
            factor = targetBrightness / max(currentBrightness, 0.01f)
            // 结合动态因子平滑调整
            factor = (factor + dynamicFactor) / 2.0f
        } else {
            factor = dynamicFactor
        }

        return applyAdaptiveBrightness(original, factor)
    }

    // 自适应亮度调整（不同区域不同处理）
    fun applyAdaptiveBrightness(original: NativeImage, baseFactor: Float): NativeImage {
        val result = NativeImage(original.width, original.height, false)

        for (x in 0..<original.width) {
            for (y in 0..<original.height) {
                val color = original.getPixelRGBA(x, y)
                val alpha = (color shr 24) and 0xFF

                if (alpha > 10) {
                    var r = ((color shr 16) and 0xFF) / 255.0f
                    var g = ((color shr 8) and 0xFF) / 255.0f
                    var b = (color and 0xFF) / 255.0f
                    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

                    // 根据当前像素的亮度动态调整系数
                    // 暗部提亮更多，亮部提亮更少
                    val localFactor: Float = if (luminance < 0.2f) {
                        // 暗部：提亮更多
                        baseFactor * 1.1f
                    } else if (luminance < 0.5f) {
                        // 中间调：正常提亮
                        baseFactor
                    } else if (luminance < 0.8f) {
                        // 亮部：少提亮
                        baseFactor * 0.95f
                    } else {
                        // 高光：几乎不提亮
                        baseFactor * 0.9f
                    }

                    // 应用调整，使用曲线调整避免过曝
                    r = applyBrightnessCurve(r + 0.05f, localFactor)
                    g = applyBrightnessCurve(g + 0.05f, localFactor)
                    b = applyBrightnessCurve(b + 0.05f, localFactor)

                    val newColor = (alpha shl 24) or
                            ((r * 255).toInt() shl 16) or
                            ((g * 255).toInt() shl 8) or (b * 255).toInt()

                    result.setPixelRGBA(x, y, newColor)
                } else {
                    result.setPixelRGBA(x, y, color)
                }
            }
        }

        return result
    }

    // 使用曲线调整亮度（避免线性调整的过曝问题）
    private fun applyBrightnessCurve(value: Float, factor: Float): Float {
        return if (factor >= 1.0f) {
            // 提亮时使用非线性曲线，避免高光过曝
            1.0f - exp((-factor * value).toDouble()).toFloat()
        } else {
            // 压暗时使用幂函数，保持对比度
            value.toDouble().pow((1.0f / factor).toDouble()).toFloat()
        }
    }

    // 获取或创建智能调整后的纹理
    @JvmStatic
    fun getSmartBrightenedTexture(originalLoc: ResourceLocation, targetBrightness: Float): ResourceLocation {
        if (PROCESSED_TEXTURES.containsKey(originalLoc)) {
            return PROCESSED_TEXTURES[originalLoc]!!
        }

        try {
            val resourceManager = mc.resourceManager
            val resource = resourceManager.getResource(originalLoc).orElseThrow()

            val originalImage = NativeImage.read(resource.open())
            val brightenedImage = smartBrighten(originalImage, targetBrightness)
            originalImage.close()

            val newTextureLoc = ResourceLocation(
                originalLoc.namespace,
                originalLoc.path.replace(".png", "_smartbright.png")
            )

            Minecraft.getInstance().getTextureManager().register(
                newTextureLoc,
                DynamicTexture(brightenedImage)
            )

            // 计算并存储实际使用的亮度系数
            val usedFactor = calculateDynamicBrightnessFactor(brightenedImage)
            BRIGHTNESS_CACHE[originalLoc] = usedFactor
            PROCESSED_TEXTURES[originalLoc] = newTextureLoc

            brightenedImage.close()

            return newTextureLoc
        } catch (e: Exception) {
            e.printStackTrace()
            return originalLoc
        }
    }

    // 枚举：亮度分类
    enum class BrightnessCategory {
        DARK, NORMAL, BRIGHT
    }
}