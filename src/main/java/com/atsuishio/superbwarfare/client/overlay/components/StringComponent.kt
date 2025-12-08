package com.atsuishio.superbwarfare.client.overlay.components

import com.atsuishio.superbwarfare.client.overlay.RenderContext
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class StringComponent(
    baseAnchorPoint: AnchorPoint = CENTER,
    componentAnchorPoint: AnchorPoint = LEFT_TOP,
) : BaseComponent(baseAnchorPoint, componentAnchorPoint) {

    override val width
        get() = font.splitter.stringWidth(component.visualOrderText)

    override val height
        get() = font.lineHeight.toFloat()

    var color = -1
    var dropShadow = false
    var font = com.atsuishio.superbwarfare.tools.font
    var component: MutableComponent = Component.empty()

    override fun RenderContext.renderComponent() {
        guiGraphics.drawString(font, component.visualOrderText, x, y, color, dropShadow)
    }
}