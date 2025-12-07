package com.atsuishio.superbwarfare.client.overlay.components

import com.atsuishio.superbwarfare.client.overlay.RenderContext
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

class StringComponent(
    anchorPoint: AnchorPoint,
    val font: Font = com.atsuishio.superbwarfare.tools.font,
    val color: Int = -1,
    val dropShadow: Boolean = false
) : BaseComponent(anchorPoint) {

    // TODO 能否隐式传递RenderContext进去？
    fun render(
        context: RenderContext,
        component: Component,
        font: Font = this@StringComponent.font,
        color: Int = this@StringComponent.color,
        dropShadow: Boolean = this@StringComponent.dropShadow
    ) {
        context.run {
            guiGraphics.drawString(font, component.visualOrderText, x, y, color, dropShadow)
        }
    }
}