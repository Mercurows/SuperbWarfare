package com.atsuishio.superbwarfare.client.overlay.components

val LEFT_TOP = AnchorPoint({ 0F }, { 0F }, { 0F }, { 0F })
val LEFT_BOTTOM = AnchorPoint({ 0F }, { it }, { 0F }, { -it })
val RIGHT_TOP = AnchorPoint({ it }, { 0F }, { -it }, { 0F })
val RIGHT_BOTTOM = AnchorPoint({ it }, { it }, { -it }, { -it })

val CENTER_TOP = AnchorPoint({ it / 2 }, { 0F }, { -it / 2 }, { 0F })
val CENTER_BOTTOM = AnchorPoint({ it / 2 }, { it }, { -it / 2 }, { -it })

val LEFT_CENTER = AnchorPoint({ 0F }, { it / 2 }, { 0F }, { -it / 2 })
val RIGHT_CENTER = AnchorPoint({ it }, { it / 2 }, { -it }, { -it / 2 })

val CENTER = AnchorPoint({ it / 2 }, { it / 2 }, { it / 2 }, { it / 2 })

data class AnchorPoint(
    // 基础坐标点位
    val baseX: (Float) -> Float,
    val baseY: (Float) -> Float,
    // 当组件指定挂载点时，计算组件本身的位置偏移
    val componentX: (Float) -> Float,
    val componentY: (Float) -> Float,
    // 其余自定义偏移
    val xOffset: Float = 0F,
    val yOffset: Float = 0f
) {
    // TODO 修改为包装成新函数
    fun offset(x: Float, y: Float) = this.copy(xOffset = x, yOffset = y)
    fun offsetX(x: Float) = this.copy(xOffset = x)
    fun offsetY(y: Float) = this.copy(yOffset = y)

    fun addOffset(x: Float, y: Float) = this.copy(xOffset = xOffset + x, yOffset = yOffset + y)
    fun addOffsetX(x: Float) = this.copy(xOffset = xOffset + x)
    fun addOffsetY(y: Float) = this.copy(yOffset = yOffset + y)

    fun getX(width: Float) = baseX(width) + xOffset
    fun getY(height: Float) = baseY(height) + yOffset
}