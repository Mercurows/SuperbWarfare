package com.atsuishio.superbwarfare.client.overlay.components

import com.atsuishio.superbwarfare.client.overlay.RenderContext


abstract class BaseComponent(val anchorPoint: AnchorPoint) {
    // 渲染时的坐标
    val RenderContext.x get() = this@BaseComponent.anchorPoint.getX(screenWidth.toFloat())
    val RenderContext.y get() = this@BaseComponent.anchorPoint.getY(screenHeight.toFloat())

    // TODO 添加宽高计算以及基于元素宽高的相对位置计算
}