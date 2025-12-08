package com.atsuishio.superbwarfare.client.overlay.components

import com.atsuishio.superbwarfare.client.overlay.RenderContext


abstract class BaseComponent(
    val baseAnchorPoint: AnchorPoint = CENTER,
    val componentAnchorPoint: AnchorPoint = LEFT_TOP
) {
    // 渲染时的坐标
    val RenderContext.x
        get() = this@BaseComponent.baseAnchorPoint.getX(screenWidth.toFloat()) +
                this@BaseComponent.componentAnchorPoint.componentX(width) +
                xOffset

    val RenderContext.y
        get() = this@BaseComponent.baseAnchorPoint.getY(screenHeight.toFloat()) +
                this@BaseComponent.componentAnchorPoint.componentY(height) +
                yOffset

    var xOffset = 0F
    var yOffset = 0F

    abstract val width: Float
    abstract val height: Float

    abstract fun RenderContext.renderComponent()

    fun shouldRender() = true
}