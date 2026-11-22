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
) {

    val safeRange: Float get() = range.coerceIn(1f, 256f)
    val safeIntensity: Float get() = intensity.coerceIn(0f, 10f)
    val safeOuterAngle: Float get() = angle.coerceIn(1f, 170f)
    val safeInnerAngle: Float get() = innerAngle.coerceIn(0f, (safeOuterAngle - 0.1f).coerceAtLeast(0f))
    fun outerCos(): Float = cos(Math.toRadians(safeOuterAngle.toDouble() * 0.5)).toFloat()
    fun innerCos(): Float = cos(Math.toRadians(safeInnerAngle.toDouble() * 0.5)).toFloat()
    fun colorRgb(): Int = color.get() and 0xFFFFFF
}
