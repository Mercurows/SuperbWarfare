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
) {
    // TODO 如何处理组件挂载位置？

    fun offset(x: Float, y: Float) =
        this.copy(baseX = { width -> baseX(width) + x }, baseY = { height -> baseY(height) + y })

    fun offsetX(x: Float) = this.copy(baseX = { width -> baseX(width) + x })
    fun offsetY(y: Float) = this.copy(baseY = { height -> baseY(height) + y })

    fun getX(width: Float) = baseX(width)
    fun getY(height: Float) = baseY(height)
}