package com.atsuishio.superbwarfare.client.screens.component

import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.getEntityTranslationKey
import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.font
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack

class RecipeButton(x: Int, y: Int, private val stack: ItemStack, onPress: OnPress) :
    Button(x, y, 80, 18, Component.empty(), onPress, DEFAULT_NARRATION) {
    private var isSelected = false

    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        pGuiGraphics.pose().pushPose()
        RenderSystem.enableDepthTest()

        if (this.isSelected) {
            if (this.isHoveredOrFocused) {
                pGuiGraphics.blit(
                    VehicleAssemblingScreen.TEXTURE,
                    this.x,
                    this.y,
                    6f,
                    239f,
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
                    6f,
                    220f,
                    this.width,
                    this.height,
                    VehicleAssemblingScreen.IMAGE_SIZE,
                    VehicleAssemblingScreen.IMAGE_SIZE
                )
            }
        } else {
            if (this.isHoveredOrFocused) {
                pGuiGraphics.blit(
                    VehicleAssemblingScreen.TEXTURE,
                    this.x,
                    this.y,
                    6f,
                    201f,
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
                    6f,
                    182f,
                    this.width,
                    this.height,
                    VehicleAssemblingScreen.IMAGE_SIZE,
                    VehicleAssemblingScreen.IMAGE_SIZE
                )
            }
        }

        pGuiGraphics.renderItem(this.stack, this.x + 2, this.y + 1)
        pGuiGraphics.renderItemDecorations(font, this.stack, this.x + 2, this.y + 1)
        val hoverName: Component?
        if (this.stack.`is`(ModItems.CONTAINER.get())) {
            val tag = BlockItem.getBlockEntityData(this.stack)
            if (tag != null && tag.contains("EntityType")) {
                val key = getEntityTranslationKey(tag.getString("EntityType"))
                hoverName = Component.translatable(key ?: "des.superbwarfare.container.empty")
            } else {
                hoverName = this.stack.getHoverName()
            }
        } else {
            hoverName = this.stack.getHoverName()
        }
        renderScrollingString(
            pGuiGraphics,
            mc.font,
            hoverName,
            this.x + 20,
            this.y + 4,
            this.x + 78,
            this.y + 13,
            16777215
        )
        pGuiGraphics.pose().popPose()
    }

    override fun onPress() {
        this.isSelected = true
        this.onPress.onPress(this)
    }

    fun setSelected(selected: Boolean) {
        this.isSelected = selected
    }

    fun renderTooltips(pGuiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (this.isHoveredOrFocused && !this.stack.isEmpty) {
            if (mouseX > this.x + 1 && mouseY > this.y + 1 && mouseX < this.x + this.width - 1 && mouseY < this.y + this.height - 1) {
                pGuiGraphics.renderTooltip(mc.font, this.stack, mouseX, mouseY)
            }
        }
    }
}
