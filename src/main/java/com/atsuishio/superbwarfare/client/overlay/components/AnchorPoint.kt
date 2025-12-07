package com.atsuishio.superbwarfare.client.overlay.components

val LEFT_TOP = AnchorPoint({ 0F }, { 0F })
val LEFT_BOTTOM = AnchorPoint({ 0F }, { it })
val RIGHT_TOP = AnchorPoint({ it }, { 0F })
val RIGHT_BOTTOM = AnchorPoint({ it }, { it })

val CENTER_TOP = AnchorPoint({ it / 2 }, { 0F })
val CENTER_BOTTOM = AnchorPoint({ it / 2 }, { it })

val LEFT_CENTER = AnchorPoint({ 0F }, { it / 2 })
val RIGHT_CENTER = AnchorPoint({ it }, { it / 2 })

val CENTER = AnchorPoint({ it / 2 }, { it / 2 })

class AnchorPoint(
    val baseX: (Float) -> Float,
    val baseY: (Float) -> Float,
    val xOffset: Float = 0F,
    val yOffset: Float = 0f
) {
    fun offset(x: Float, y: Float) = AnchorPoint(baseX, baseY, x, y)

    fun getX(width: Float) = baseX(width) + xOffset
    fun getY(height: Float) = baseY(height) + yOffset
}