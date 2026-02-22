package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.MiniVehicleContainerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class MiniVehicleContainerScreen(menu: MiniVehicleContainerMenu, inventory: Inventory, title: Component) :
    AbstractVehicleContainerScreen<MiniVehicleContainerMenu>(menu, inventory, title) {
    override fun renderBg(
        guiGraphics: GuiGraphics,
        pPartialTick: Float,
        pMouseX: Int,
        pMouseY: Int
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight)
        guiGraphics.blit(INV, i, j + 23 + this.menu.getRows() * 18, 0, 0, 176, 90)
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/vehicle/inventory/mini.png")
    }
}