package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.MediumVehicleContainerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class MediumVehicleContainerScreen(menu: MediumVehicleContainerMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<MediumVehicleContainerMenu>(menu, inventory, title) {
    init {
        this.imageWidth = 176
        this.imageHeight = 166
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        pPartialTick: Float,
        pMouseX: Int,
        pMouseY: Int
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight)
    }

    override fun render(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        this.renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        this.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/vehicle/inventory/medium.png")
    }
}