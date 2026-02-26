package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.HugeVehicleContainerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class HugeVehicleContainerScreen(menu: HugeVehicleContainerMenu, inventory: Inventory, title: Component) :
    AbstractVehicleContainerScreen<HugeVehicleContainerMenu>(menu, inventory, title) {
    override fun init() {
        super.init()
        this.imageWidth = 320
        this.imageHeight = 222
        this.leftPos = (this.width - this.imageWidth) / 2 + 72
        this.titleLabelX = -64
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        pPartialTick: Float,
        pMouseX: Int,
        pMouseY: Int
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        guiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 328, 328)
        guiGraphics.blit(INV, i + 72, j + 23 + this.menu.getRows() * 18, 0, 0, 176, 90)
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/vehicle/inventory/huge.png")
    }
}