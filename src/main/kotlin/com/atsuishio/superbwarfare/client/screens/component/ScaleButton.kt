package com.atsuishio.superbwarfare.client.screens.component

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class ScaleButton(x: Int, y: Int, private val uOffset: Int, private val vOffset: Int, onPress: OnPress) :
    Button(x, y, 9, 9, Component.empty(), onPress, DEFAULT_NARRATION) {
    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        if (this.isHovered()) {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                uOffset.toFloat(),
                (vOffset + 10).toFloat(),
                this.width,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        } else {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                uOffset.toFloat(),
                vOffset.toFloat(),
                this.width,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        }
    }
}
