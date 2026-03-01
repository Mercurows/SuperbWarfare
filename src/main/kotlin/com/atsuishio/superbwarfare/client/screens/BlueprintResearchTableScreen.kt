package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_ADDITION
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_BASE
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_INPUT
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity.Companion.SLOT_SPECIAL
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.network.message.send.BlueprintCraftMessage
import com.atsuishio.superbwarfare.network.message.send.BlueprintSetIndexMessage
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
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.jvm.optionals.getOrNull

@OnlyIn(Dist.CLIENT)
class BlueprintResearchTableScreen(
    menu: BlueprintResearchTableMenu, playerInventory: Inventory, title: Component
) : AbstractContainerScreen<BlueprintResearchTableMenu>(menu, playerInventory, title) {
    private var currentResultList: MutableList<Item> = mutableListOf()
    private var currentPage: Int = 0

    init {
        this.imageWidth = 240
        this.imageHeight = 177
        this.titleLabelY = 2
    }

    override fun init() {
        super.init()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        this.addRenderableWidget(CraftButton(i + 73, j + 75))
        this.addRenderableWidget(PageButton(i + 182, j + 160, false))
        this.addRenderableWidget(PageButton(i + 207, j + 160, true))
    }

    override fun renderLabels(
        pGuiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int
    ) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false)
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
        this.renderBackground(guiGraphics)
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

        // 指示灯
        val activated = this.menu.isActivated()
        if (activated) {
            guiGraphics.blit(TEXTURE, i + 91, j + 77, 11, 242, 2, 2)
        } else {
            guiGraphics.blit(TEXTURE, i + 99, j + 77, 19, 242, 2, 2)
        }

        // 模块指示灯
        val recipe = this.getRecipe() ?: return
        val color = recipe.color
        if (color == 0 || color > 4) return
        val colorU = when (color) {
            1, 3 -> 129
            else -> 166
        }
        val colorV = when (color) {
            1, 2 -> 178
            else -> 203
        }
        guiGraphics.blit(TEXTURE, i + 70, j + 46, colorU, colorV, 36, 24)
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

                val itemX = i + 182 + ix % 3 * 17
                val itemY = j + 8 + (ix / 3) % 9 * 17
                val stack = this.currentResultList[index].defaultInstance

                guiGraphics.renderFakeItem(stack, itemX, itemY)

                if (recipe.selectable) {
                    val selectedIndex = this.menu.getLastSelectedIndex()
                    if (selectedIndex == index) {
                        guiGraphics.blit(TEXTURE, itemX - 1, itemY - 1, 207, 215, 18, 18)
                    }
                }

                if (mouseX in itemX..itemX + 16 && mouseY in itemY..itemY + 16) {
                    if (recipe.selectable) {
                        guiGraphics.blit(TEXTURE, itemX - 1, itemY - 1, 207, 196, 18, 18)
                    }

                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY)
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, pButton: Int): Boolean {
        if (!this.currentResultList.isEmpty()) {
            if (this.getRecipe()?.selectable == false || this.menu.isActivated()) return super.mouseClicked(
                mouseX,
                mouseY,
                pButton
            )

            val i = (this.width - this.imageWidth) / 2
            val j = (this.height - this.imageHeight) / 2

            for (ix in 0 until PAGE_SIZE) {
                val index = this.currentPage * PAGE_SIZE + ix
                if (index >= this.currentResultList.size) break

                val itemX = i + 182 + ix % 3 * 17
                val itemY = j + 8 + (ix / 3) % 9 * 17

                if (mouseX in itemX.toDouble()..itemX + 16.0 && mouseY in itemY.toDouble()..itemY + 16.0) {
                    sendPacketToServer(BlueprintSetIndexMessage(index))
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, pButton)
    }

    fun getRecipe(): ResearchingRecipe? {
        val level = clientLevel ?: return null
        val manager = level.recipeManager

        val inventory = SimpleContainer(4)
        inventory.setItem(0, this.menu.getSlot(SLOT_INPUT).item)
        inventory.setItem(1, this.menu.getSlot(SLOT_BASE).item)
        inventory.setItem(2, this.menu.getSlot(SLOT_ADDITION).item)
        inventory.setItem(3, this.menu.getSlot(SLOT_SPECIAL).item)

        val optionalRecipe = manager.getRecipeFor(
            ModRecipes.RESEARCHING_TYPE.get(),
            inventory,
            level
        )
        return optionalRecipe.getOrNull()
    }

    companion object {
        val TEXTURE: ResourceLocation = Mod.loc("textures/gui/blueprint_research_table.png")
        const val PAGE_SIZE = 27
    }

    private class CraftButton(x: Int, y: Int) : AbstractButton(x, y, 10, 10, Component.empty()) {
        override fun onPress() {
            sendPacketToServer(BlueprintCraftMessage)
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
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

    private inner class PageButton(x: Int, y: Int, val forward: Boolean) :
        AbstractButton(x, y, 25, 8, Component.empty()) {
        override fun onPress() {
            val recipe = this@BlueprintResearchTableScreen.getRecipe() ?: return
            if (!recipe.result.isRandom()) return
            val size = recipe.result.getResultList().size
            if (size < PAGE_SIZE) return
            val pages = size / PAGE_SIZE
            if (forward) {
                this@BlueprintResearchTableScreen.currentPage =
                    (this@BlueprintResearchTableScreen.currentPage + 1).coerceAtMost(pages)
            } else {
                this@BlueprintResearchTableScreen.currentPage =
                    (this@BlueprintResearchTableScreen.currentPage - 1).coerceAtLeast(0)
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        override fun renderWidget(
            pGuiGraphics: GuiGraphics,
            pMouseX: Int,
            pMouseY: Int,
            pPartialTick: Float
        ) {
            if (!this.isHovered) return

            val recipe = this@BlueprintResearchTableScreen.getRecipe() ?: return
            if (!recipe.result.isRandom()) return
            val size = recipe.result.getResultList().size
            val pages = size / PAGE_SIZE
            if (this.forward) {
                if (this@BlueprintResearchTableScreen.currentPage < pages) {
                    pGuiGraphics.blit(TEXTURE, this.x, this.y, 207, 178, 25, 8)
                }
            } else if (this@BlueprintResearchTableScreen.currentPage > 0) {
                pGuiGraphics.blit(TEXTURE, this.x, this.y, 207, 187, 25, 8)
            }
        }
    }
}