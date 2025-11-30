package com.atsuishio.superbwarfare.tools

import net.minecraft.util.Mth
import kotlin.math.*

fun ClosedRange<Int>.lerp(delta: Double) = Mth.clamp(delta, start.toDouble(), endInclusive.toDouble())
fun ClosedRange<Int>.lerp(delta: Float) = lerp(delta.toDouble())
fun ClosedFloatingPointRange<Double>.lerp(delta: Double) = Mth.lerp(delta, start, endInclusive)
fun ClosedFloatingPointRange<Double>.lerp(delta: Float) = lerp(delta.toDouble())

object MathTool {
    /**
     * 大小逐渐减弱到0的震荡函数
     * @param a 初始振幅
     * @param t 持续时间（秒）
     * @param c 震荡频率（Hz）
     * @param elapsedTime 已过去的时间（秒）
     * @return 当前时刻的震荡值
     */
    @JvmStatic
    fun decayingOscillation(a: Float, t: Float, c: Float, elapsedTime: Float): Float {
        // 如果时间已超过持续时间，返回0
        if (elapsedTime >= t) {
            return 0.0f
        }

        // 计算衰减因子（指数衰减）
        val decayFactor = exp((-3.0f * elapsedTime / t).toDouble()).toFloat()

        // 计算震荡部分（正弦波）
        val oscillation = sin(2.0f * Math.PI * c * elapsedTime).toFloat()

        // 返回衰减后的震荡值
        return a * decayFactor * oscillation
    }

    /**
     * 重载版本，使用Minecraft的tick时间系统
     * @param a 初始振幅
     * @param t 持续时间（秒）
     * @param c 震荡频率（Hz）
     * @param ticks 已过去的tick数
     * @return 当前时刻的震荡值
     */
    fun decayingOscillation(a: Float, t: Float, c: Float, ticks: Int): Float {
        // 将tick转换为秒（Minecraft中20ticks=1秒）
        val elapsedTime = ticks / 20.0f
        return decayingOscillation(a, t, c, elapsedTime)
    }

    /**
     * 获取渐变颜色
     * @param startColor 起始颜色 (16进制RGB)
     * @param endColor 结束颜色 (16进制RGB)
     * @param progress 渐变进度 (0-100)
     * @param mode 渐变模式 (HSV或HSL)
     * @return 渐变后的颜色 (16进制RGB)
     */
    @JvmStatic
    fun getGradientColor(startColor: Int, endColor: Int, progress: Int, mode: Int): Int {
        // 确保进度在0-100范围内
        var progress = progress
        progress = max(0, min(100, progress))
        val ratio = progress / 100.0f

        return if (mode == 2) {
            hsvGradient(startColor, endColor, ratio)
        } else {
            hslGradient(startColor, endColor, ratio)
        }
    }

    /**
     * 在HSV颜色空间中进行渐变
     */
    private fun hsvGradient(startColor: Int, endColor: Int, ratio: Float): Int {
        // 将RGB转换为HSV
        val startHSV = rgbToHsv(startColor)
        val endHSV = rgbToHsv(endColor)

        // 对HSV分量进行插值
        // 对于色相(H)，需要考虑色相环的循环特性
        val h = interpolateHue(startHSV[0], endHSV[0], ratio)
        val s = startHSV[1] + (endHSV[1] - startHSV[1]) * ratio
        val v = startHSV[2] + (endHSV[2] - startHSV[2]) * ratio

        // 将HSV转换回RGB
        return hsvToRgb(h, s, v)
    }

    /**
     * 在HSL颜色空间中进行渐变
     */
    private fun hslGradient(startColor: Int, endColor: Int, ratio: Float): Int {
        // 将RGB转换为HSL
        val startHSL = rgbToHsl(startColor)
        val endHSL = rgbToHsl(endColor)

        // 对HSL分量进行插值
        // 对于色相(H)，需要考虑色相环的循环特性
        val h = interpolateHue(startHSL[0], endHSL[0], ratio)
        val s = startHSL[1] + (endHSL[1] - startHSL[1]) * ratio
        val l = startHSL[2] + (endHSL[2] - startHSL[2]) * ratio

        // 将HSL转换回RGB
        return hslToRgb(h, s, l)
    }

    /**
     * 插值色相值，考虑色相环的循环特性
     */
    private fun interpolateHue(startH: Float, endH: Float, ratio: Float): Float {
        // 确保色相值在0-1范围内
        var startH = startH
        var endH = endH
        startH %= 1.0f
        endH %= 1.0f

        // 计算两个方向的差值
        val diff = endH - startH

        // 如果差值大于0.5，说明应该从另一个方向绕色相环
        if (abs(diff) > 0.5f) {
            if (diff > 0) {
                startH += 1.0f
            } else {
                endH += 1.0f
            }
        }

        // 线性插值
        return (startH + (endH - startH) * ratio) % 1.0f
    }

    /**
     * 将RGB颜色转换为HSV
     * @param rgb RGB颜色
     * @return 包含H(0 - 1), S(0-1), V(0-1)的数组
     */
    private fun rgbToHsv(rgb: Int): FloatArray {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF

        val rNorm = r / 255.0f
        val gNorm = g / 255.0f
        val bNorm = b / 255.0f

        val max = max(rNorm, max(gNorm, bNorm))
        val min = min(rNorm, min(gNorm, bNorm))
        val delta = max - min

        var h = 0f
        if (delta != 0f) {
            h = when (max) {
                rNorm -> (gNorm - bNorm) / delta % 6
                gNorm -> (bNorm - rNorm) / delta + 2
                else -> (rNorm - gNorm) / delta + 4
            }
            h /= 6f
            if (h < 0) h += 1f
        }

        val s = if (max == 0f) 0f else delta / max

        return floatArrayOf(h, s, max)
    }

    /**
     * 将HSV转换为RGB
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        var h = h % 1.0f
        if (h < 0) h += 1.0f

        val hi = (h * 6).toInt()
        val f = h * 6 - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val r: Float
        val g: Float
        val b: Float
        when (hi) {
            0 -> {
                r = v
                g = t
                b = p
            }

            1 -> {
                r = q
                g = v
                b = p
            }

            2 -> {
                r = p
                g = v
                b = t
            }

            3 -> {
                r = p
                g = q
                b = v
            }

            4 -> {
                r = t
                g = p
                b = v
            }

            else -> {
                r = v
                g = p
                b = q
            }
        }

        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    /**
     * 将RGB颜色转换为HSL
     */
    private fun rgbToHsl(rgb: Int): FloatArray {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF

        val rNorm = r / 255.0f
        val gNorm = g / 255.0f
        val bNorm = b / 255.0f

        val max = max(rNorm, max(gNorm, bNorm))
        val min = min(rNorm, min(gNorm, bNorm))
        val delta = max - min

        var h = 0f
        if (delta != 0f) {
            h = when (max) {
                rNorm -> (gNorm - bNorm) / delta % 6
                gNorm -> (bNorm - rNorm) / delta + 2
                else -> (rNorm - gNorm) / delta + 4
            } / 6f
            if (h < 0) h += 1f
        }

        val l = (max + min) / 2
        val s = if (delta == 0f) 0f else delta / (1 - abs(2 * l - 1))

        return floatArrayOf(h, s, l)
    }

    /**
     * 将HSL转换为RGB
     */
    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        var h = h % 1.0f
        if (h < 0) h += 1.0f

        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h * 6) % 2 - 1))
        val m = l - c / 2

        var r: Float
        var g: Float
        var b: Float
        if (h < 1 / 6.0) {
            r = c
            g = x
            b = 0f
        } else if (h < 2 / 6.0) {
            r = x
            g = c
            b = 0f
        } else if (h < 3 / 6.0) {
            r = 0f
            g = c
            b = x
        } else if (h < 4 / 6.0) {
            r = 0f
            g = x
            b = c
        } else if (h < 5 / 6.0) {
            r = x
            g = 0f
            b = c
        } else {
            r = c
            g = 0f
            b = x
        }

        r += m
        g += m
        b += m

        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    /**
     * 获取渐变颜色 (默认HSV模式)
     */
    fun getGradientColor(startColor: Int, endColor: Int, progress: Int): Int {
        return getGradientColor(startColor, endColor, progress, 1)
    }

    /**
     * 将RGB颜色转换为16进制字符串
     */
    fun toHexString(color: Int): String {
        return String.format("#%06X", (0xFFFFFF and color))
    }
}
