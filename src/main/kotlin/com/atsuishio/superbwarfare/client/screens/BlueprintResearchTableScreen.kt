package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_ADDITION
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_BASE
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_INPUT
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_SPECIAL
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.network.message.send.BlueprintCraftMessage
import com.atsuishio.superbwarfare.recipe.ResearchingRecipe
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class BlueprintResearchTableScreen(
    menu: BlueprintResearchTableMenu, playerInventory: Inventory, title: Component
) : AbstractContainerScreen<BlueprintResearchTableMenu>(menu, playerInventory, title) {
    private var currentResultList: MutableList<Item> = mutableListOf()
    private var currentPage: Int = 0

    init {
        this.imageWidth = 240
        this.imageHeight = 177
    }

    override fun init() {
        super.init()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        this.addRenderableWidget(CraftButton(i + 73, j + 75))
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
        this.renderRecipeOutputs(guiGraphics, mouseX, mouseY, partialTick)
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

    fun renderRecipeOutputs(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val recipe = this.getRecipe()
        if (recipe == null) {
            this.currentPage = 0
            this.currentResultList = mutableListOf()
            return
        } else {
            val result = recipe.result
            if (!result.isRandom()) return
            this.currentResultList = result.getResultList()
        }

        if (!this.currentResultList.isEmpty()) {
            for (ix in 0 until PAGE_SIZE) {
                val index = this.currentPage * PAGE_SIZE + ix
                if (index >= this.currentResultList.size) break

                guiGraphics.renderFakeItem(
                    this.currentResultList[index].defaultInstance,
                    i + 182 + ix % 3 * 17,
                    j + 8 + (ix / 3) % 9 * 17
                )
            }
        }
    }

    fun getRecipe(): ResearchingRecipe? {
        val level = clientLevel ?: return null
        val manager = level.recipeManager

        val inventory = SimpleContainer(4)
        inventory.setItem(0, this.menu.getSlot(SLOT_INPUT).item)
        inventory.setItem(1, this.menu.getSlot(SLOT_BASE).item)
        inventory.setItem(2, this.menu.getSlot(SLOT_ADDITION).item)
        inventory.setItem(3, this.menu.getSlot(SLOT_SPECIAL).item)

        // TODO 怎么实现这玩意
        return null
//        val optionalRecipe = manager.getRecipeFor(
//            ModRecipes.RESEARCHING_TYPE.get(),
//            inventory,
//            level
//        )
//        return optionalRecipe.getOrNull()
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/blueprint_research_table.png")
        const val PAGE_SIZE = 27
    }

    private class CraftButton(x: Int, y: Int) : AbstractButton(x, y, 10, 10, Component.empty()) {
        override fun onPress() {
            sendPacketToServer(BlueprintCraftMessage)
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput?) {
        }

        override fun renderWidget(
            pGuiGraphics: GuiGraphics,
            pMouseX: Int,
            pMouseY: Int,
            pPartialTick: Float
        ) {
            if (!this.isHovered) {
                pGuiGraphics.blit(TEXTURE, this.x, this.y, 0, 234, 9, 9)
            }
        }
    }
}