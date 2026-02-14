package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import kotlin.math.max

open class ClientCellImageTooltip(tooltip: CellImageComponent) : ClientTooltipComponent {
    protected val tipWidth: Int = tooltip.width
    protected val tipHeight: Int = tooltip.height
    protected val stack: ItemStack = tooltip.stack

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        guiGraphics.pose().pushPose()
        if (shouldRenderEnergyTooltip()) {
            renderEnergyTooltip(font, guiGraphics, x, y)
        }

        guiGraphics.pose().popPose()
    }

    protected fun shouldRenderEnergyTooltip(): Boolean {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent && stack.getCapability(ForgeCapabilities.ENERGY)
            .resolve().isPresent
    }

    protected fun renderEnergyTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.energyComponent, x, y, 0xFFFFFF)
    }

    protected val energyComponent: Component
        get() {
            assert(stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent)
            val storage = stack.getCapability(ForgeCapabilities.ENERGY).resolve().get()
            val energy = storage.energyStored
            val maxEnergy = storage.maxEnergyStored
            val percentage = (energy.toFloat() / maxEnergy).coerceIn(0f, 1f)
            val component = Component.empty()
            val format = if (percentage <= .2f) {
                ChatFormatting.RED
            } else if (percentage <= .6f) {
                ChatFormatting.YELLOW
            } else {
                ChatFormatting.GREEN
            }

            val count = (percentage * 50).toInt()
            repeat(count) {
                component.append(Component.literal("|").withStyle(format))
            }
            component.append(Component.empty().withStyle(ChatFormatting.RESET))
            repeat(50 - count) {
                component.append(Component.literal("|").withStyle(ChatFormatting.GRAY))
            }

            component.append(
                Component.literal(" $energy/$maxEnergy FE")
                    .withStyle(ChatFormatting.GRAY)
            )

            return component
        }

    override fun getHeight(): Int {
        var height = 20
        if (shouldRenderEnergyTooltip()) height -= 10
        return height
    }

    override fun getWidth(font: Font): Int {
        var width = if (Screen.hasShiftDown()) {
            max(this.tipWidth, 20)
        } else {
            20
        }

        if (shouldRenderEnergyTooltip()) width = max(width, font.width(this.energyComponent.visualOrderText) + 10)
        return width
    }
}
