package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu
import com.atsuishio.superbwarfare.network.message.send.ShowChargingRangeMessage
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

@OnlyIn(Dist.CLIENT)
class ChargingStationScreen(pMenu: ChargingStationMenu, pPlayerInventory: Inventory, pTitle: Component) :
    AbstractContainerScreen<ChargingStationMenu>(pMenu, pPlayerInventory, pTitle) {
    init {
        imageWidth = 176
        imageHeight = 166
    }

    override fun renderBg(pGuiGraphics: GuiGraphics, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight)

        val fuelTick = this.menu!!.fuelTick
        var maxFuelTick = this.menu!!.maxFuelTick
        val energy = this.menu!!.energy

        if (maxFuelTick == 0L) {
            maxFuelTick = ChargingStationBlockEntity.DEFAULT_FUEL_TIME.toLong()
        }

        // Fuel
        val fuelRate = fuelTick.toFloat() / maxFuelTick.toFloat()
        pGuiGraphics.blit(
            TEXTURE,
            i + 45,
            j + 51 - (13 * fuelRate).toInt(),
            177,
            14 - (13 * fuelRate).toInt(),
            13,
            (13 * fuelRate).toInt()
        )

        // Energy
        val energyRate = energy.toFloat() / ChargingStationBlockEntity.MAX_ENERGY.toFloat()
        pGuiGraphics.blit(
            TEXTURE, i + 80, j + 70 - (54 * energyRate).toInt(),
            177, 17, 16, (54 * energyRate).toInt()
        )
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY)
    }

    override fun renderTooltip(pGuiGraphics: GuiGraphics, pX: Int, pY: Int) {
        super.renderTooltip(pGuiGraphics, pX, pY)

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val tooltip: MutableList<Component> = arrayListOf()
        tooltip.add(
            Component.translatable(
                "des.superbwarfare.charging_station.energy", this.menu!!.energy,
                ChargingStationBlockEntity.MAX_ENERGY
            )
        )

        if (pX - i in 80..96 && pY - j in 16..70) {
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY)
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class ShowRangeButton(pX: Int, pY: Int) : AbstractButton(
        pX + 7,
        pY + 55,
        33,
        14,
        Component.translatable("container.superbwarfare.charging_station.show_range")
    ) {
        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            this.message = if (this@ChargingStationScreen.menu!!.showRange())
                Component.translatable("container.superbwarfare.charging_station.hide_range")
            else Component.translatable("container.superbwarfare.charging_station.show_range")
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        }

        override fun onPress() {
            sendPacketToServer(ShowChargingRangeMessage(!this@ChargingStationScreen.menu!!.showRange()))
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    override fun init() {
        super.init()
        this.titleLabelX = 8
        this.titleLabelY = 5
        this.inventoryLabelX = 8
        this.inventoryLabelY = 74

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        this.addRenderableWidget(ShowRangeButton(i, j))
    }

    companion object {
        private val TEXTURE = loc("textures/gui/charging_station.png")
    }
}
