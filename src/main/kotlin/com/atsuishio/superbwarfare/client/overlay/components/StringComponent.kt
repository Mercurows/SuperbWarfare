package com.atsuishio.superbwarfare.client.overlay.components

import com.atsuishio.superbwarfare.client.overlay.RenderContext
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class StringComponent(
    baseAnchorPoint: AnchorPoint = CENTER,
    componentAnchorPoint: AnchorPoint = LEFT_TOP,
    var font: Font = com.atsuishio.superbwarfare.tools.font,
    var component: MutableComponent = Component.empty(),
    var color: Int = -1,
    var dropShadow: Boolean = false,
) : BaseComponent(baseAnchorPoint, componentAnchorPoint) {

    override val width
        get() = font.splitter.stringWidth(component.visualOrderText)

    override val height
        get() = font.lineHeight.toFloat()

    override fun RenderContext.renderComponent() {
        guiGraphics.drawString(font, component.visualOrderText, x, y, color, dropShadow)
    }
}