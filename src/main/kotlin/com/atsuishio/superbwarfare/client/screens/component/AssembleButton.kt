package com.atsuishio.superbwarfare.client.screens.component

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class AssembleButton(x: Int, y: Int, onPress: OnPress) :
    Button(x, y, 56, 13, Component.empty(), onPress, DEFAULT_NARRATION) {
    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        pGuiGraphics.pose().pushPose()
        RenderSystem.enableDepthTest()

        if (this.isHovered()) {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                295f,
                196f,
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
                295f,
                182f,
                this.width,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        }

        val name: Component = Component.translatable("container.superbwarfare.vehicle_assembling_table.assemble")
        renderScrollingString(
            pGuiGraphics,
            mc.font,
            name,
            this.x + 13,
            this.y + 3,
            this.x + 56,
            this.y + 10,
            -1
        )

        pGuiGraphics.pose().popPose()
    }
}
