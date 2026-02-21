package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.AbstractVehicleContainerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

abstract class AbstractVehicleContainerScreen<T : AbstractVehicleContainerMenu>(
    menu: T,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<T>(menu, inventory, title) {
    init {
        this.imageWidth = 176
        this.imageHeight = 114 + this.menu.getRows() * 18
    }

    override fun renderLabels(
        pGuiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int
    ) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false)
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
        val INV: ResourceLocation = Mod.loc("textures/gui/vehicle/inventory/player_inventory.png")
    }
}