package com.atsuishio.superbwarfare.client.screens.component

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class PageButton(x: Int, y: Int, private val left: Boolean, onPress: OnPress) :
    Button(x, y, 10, 15, Component.empty(), onPress, DEFAULT_NARRATION) {
    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        pGuiGraphics.pose().pushPose()
        RenderSystem.enableDepthTest()

        val vOffset = if (this.left) 212 else 196

        pGuiGraphics.pose().translate(0f, 0.25f, 0f)

        if (!this.active) {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                109f,
                vOffset.toFloat(),
                this.width,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        } else {
            if (this.isHoveredOrFocused) {
                pGuiGraphics.blit(
                    VehicleAssemblingScreen.TEXTURE,
                    this.x,
                    this.y,
                    98f,
                    vOffset.toFloat(),
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
                    87f,
                    vOffset.toFloat(),
                    this.width,
                    this.height,
                    VehicleAssemblingScreen.IMAGE_SIZE,
                    VehicleAssemblingScreen.IMAGE_SIZE
                )
            }
        }

        pGuiGraphics.pose().popPose()
    }
}
