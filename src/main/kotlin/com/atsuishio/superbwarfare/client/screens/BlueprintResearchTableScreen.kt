package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BlueprintResearchTableScreen(
    menu: BlueprintResearchTableMenu, playerInventory: Inventory, title: Component
) : AbstractContainerScreen<BlueprintResearchTableMenu>(menu, playerInventory, title) {
    init {
        this.imageWidth = 240
        this.imageHeight = 177
    }

    override fun renderLabels(
        pGuiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int
    ) {
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight)
        this.renderProgresses(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun render(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        this.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    fun renderProgresses(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        // 燃料槽的红色外框
        if (this.menu.getSlot(BlueprintResearchTableMenu.SLOT_FUEL).hasItem()) {
            guiGraphics.blit(TEXTURE, i + 29, j + 19, 0, 178, 35, 20)
        }

        // 燃料条
        val fuelRate = this.menu.getFuel() / BlueprintResearchTableBlockEntity.MAX_FUEL.toDouble()
        guiGraphics.blit(TEXTURE, i + 68, j + 27, 11, 237, (40 * fuelRate).toInt(), 4)

        // 输出槽的蓝色指示灯
        if (this.menu.getSlot(BlueprintResearchTableMenu.SLOT_OUTPUT).hasItem()) {
            guiGraphics.blit(TEXTURE, i + 127, j + 40, 11, 234, 20, 2)
        }

        // 整体进度条
        val progressRate = (this.menu.getTick() / this.menu.getMaxProcessTick().toDouble()).coerceIn(0.0, 1.0)
        guiGraphics.blit(TEXTURE, i + 25, j + 42, 0, 200, (128 * progressRate).toInt(), 32)
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/blueprint_research_table.png")
    }
}