package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.ModColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.cos

@Serializable
data class FlashlightInfo(
    @SerialName("Locator")
    val locator: String = "light_pos",

    @SerialName("Range")
    val range: Float = 32f,

    @SerialName("Angle")
    val angle: Float = 30f,

    @SerialName("InnerAngle")
    val innerAngle: Float = 10f,

    @SerialName("Intensity")
    val intensity: Float = 1f,

    @SerialName("Color")
    val color: ModColor = ModColor(0xFFFFFF),

    /** 开灯瞬间的亮度倍率（1 = 不做眩光高亮）。 */
    @SerialName("FlashPeak")
    val flashPeak: Float = 8f,

    /** 从峰值回落到正常亮度的时间（秒）。 */
    @SerialName("FlashTime")
    val flashTime: Float = 0.4f,

    /** 爆闪频率（Hz）。 */
    @SerialName("StrobeHz")
    val strobeHz: Float = 12f,
) {

    val safeRange: Float get() = range.coerceIn(1f, 256f)
    val safeIntensity: Float get() = intensity.coerceIn(0f, 100f)
    val safeOuterAngle: Float get() = angle.coerceIn(1f, 170f)
    val safeInnerAngle: Float get() = innerAngle.coerceIn(0f, (safeOuterAngle - 0.1f).coerceAtLeast(0f))
    val safeFlashPeak: Float get() = flashPeak.coerceIn(1f, 100f)
    val safeFlashTime: Float get() = flashTime.coerceIn(0.02f, 2f)
    val safeStrobeHz: Float get() = strobeHz.coerceIn(1f, 30f)
    fun outerCos(): Float = cos(Math.toRadians(safeOuterAngle.toDouble() * 0.5)).toFloat()
    fun innerCos(): Float = cos(Math.toRadians(safeInnerAngle.toDouble() * 0.5)).toFloat()
    fun colorRgb(): Int = color.get() and 0xFFFFFF
}
