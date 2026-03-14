package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.inventory.menu.SuperbItemInterfaceMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class SuperbItemInterfaceScreen(menu: SuperbItemInterfaceMenu, playerInventory: Inventory, title: Component) :
    AbstractContainerScreen<SuperbItemInterfaceMenu>(menu, playerInventory, title) {
    init {
        this.imageHeight = 133
        this.inventoryLabelY = this.imageHeight - 94
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        this.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        guiGraphics.blit(HOPPER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight)
    }

    companion object {
        private val HOPPER_LOCATION = ResourceLocation("textures/gui/container/hopper.png")
    }
}
