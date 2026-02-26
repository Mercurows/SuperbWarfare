package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.LargeVehicleContainerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class LargeVehicleContainerScreen(menu: LargeVehicleContainerMenu, inventory: Inventory, title: Component) :
    AbstractVehicleContainerScreen<LargeVehicleContainerMenu>(menu, inventory, title) {
    override fun init() {
        super.init()
        this.imageWidth = 248
        // 感谢小雪宝宝帮助debug；；感谢小雪宝宝帮助debug；；感谢小雪宝宝帮助debug；；感谢小雪宝宝帮助debug；；感谢小雪宝宝帮助debug；；感谢小雪宝宝帮助debug；；
        this.leftPos = (this.width - this.imageWidth) / 2 + 36
        this.titleLabelX = -28
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
        guiGraphics.blit(INV, i + 36, j + 23 + this.menu.getRows() * 18, 0, 0, 176, 90)
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/vehicle/inventory/large.png")
    }
}