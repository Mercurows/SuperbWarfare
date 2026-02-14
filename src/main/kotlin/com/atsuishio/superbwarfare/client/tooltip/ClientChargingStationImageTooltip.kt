package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent
import com.atsuishio.superbwarfare.config.server.MiscConfig
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import kotlin.math.max

open class ClientChargingStationImageTooltip(tooltip: GunImageComponent) : ClientTooltipComponent {
    protected val tipWidth: Int = tooltip.width
    protected val tipHeight: Int = tooltip.height
    protected val stack: ItemStack = tooltip.stack

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        guiGraphics.pose().pushPose()
        renderEnergyTooltip(font, guiGraphics, x, y)
        guiGraphics.pose().popPose()
    }

    protected fun renderEnergyTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.energyComponent, x, y, 0xFFFFFF)
    }

    protected val energyComponent: Component
        get() {
            val tag = BlockItem.getBlockEntityData(stack)
            val energy = tag?.getInt("Energy") ?: 0
            val maxEnergy = max(1, MiscConfig.CHARGING_STATION_MAX_ENERGY.get())
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
                Component.literal(" $energy/$maxEnergy FE").withStyle(ChatFormatting.GRAY)
            )

            return component
        }

    override fun getHeight(): Int {
        return max(20, this.tipHeight) - 10
    }

    override fun getWidth(font: Font): Int {
        if (Screen.hasShiftDown()) {
            return max(this.tipWidth, 20)
        }
        return 20
    }
}
