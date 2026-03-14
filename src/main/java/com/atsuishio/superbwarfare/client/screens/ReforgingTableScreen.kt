package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu
import com.atsuishio.superbwarfare.network.message.send.GunReforgeMessage
import com.atsuishio.superbwarfare.network.message.send.SetPerkLevelMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
open class ReforgingTableScreen(pMenu: ReforgingTableMenu, pPlayerInventory: Inventory, pTitle: Component) :
    AbstractContainerScreen<ReforgingTableMenu>(pMenu, pPlayerInventory, pTitle) {
    init {
        imageWidth = 176
        imageHeight = 177
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTicks: Float, gx: Int, gy: Int) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0f, 0f, this.imageWidth, this.imageHeight, 200, 200)
        RenderSystem.disableBlend()
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        val ammoPerkLevel = this@ReforgingTableScreen.menu!!.ammoPerkLevel.get()
        val funcPerkLevel = this@ReforgingTableScreen.menu!!.funcPerkLevel.get()
        val damagePerkLevel = this@ReforgingTableScreen.menu!!.damagePerkLevel.get()

        if (ammoPerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 136, this.topPos + 31, 1, 178, ammoPerkLevel)
        }

        if (funcPerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 146, this.topPos + 31, 1, 184, funcPerkLevel)
        }

        if (damagePerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 156, this.topPos + 31, 1, 190, damagePerkLevel)
        }

        val upgradePoint = this@ReforgingTableScreen.menu!!.availableLevel()
        renderNumber(pGuiGraphics, this.leftPos + 43, this.topPos + 20, 51, 178, upgradePoint)

        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY)
    }

    private fun renderNumber(guiGraphics: GuiGraphics, x: Int, y: Int, u: Int, v: Int, number: Int) {
        val g = number / 10
        val s = number % 10
        guiGraphics.blit(TEXTURE, x, y, (u + 5 * g).toFloat(), v.toFloat(), 5, 5, 200, 200)
        guiGraphics.blit(TEXTURE, x + 4, y, (u + 5 * s).toFloat(), v.toFloat(), 5, 5, 200, 200)
    }

    override fun init() {
        super.init()
        this.titleLabelX = 8
        this.titleLabelY = 2
        this.inventoryLabelX = 8
        this.inventoryLabelY = 85

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val button = ReforgeButton(i + 124, j + 70)
        val ammoUpgrade = UpgradeButton(i + 98, j + 21, Perk.Type.AMMO)
        val ammoDowngrade = DowngradeButton(i + 69, j + 21, Perk.Type.AMMO)
        val funcUpgrade = UpgradeButton(i + 98, j + 41, Perk.Type.FUNCTIONAL)
        val funcDowngrade = DowngradeButton(i + 69, j + 41, Perk.Type.FUNCTIONAL)
        val damageUpgrade = UpgradeButton(i + 98, j + 61, Perk.Type.DAMAGE)
        val damageDowngrade = DowngradeButton(i + 69, j + 61, Perk.Type.DAMAGE)

        this.addRenderableWidget<ReforgeButton?>(button)
        this.addRenderableWidget<UpgradeButton?>(ammoUpgrade)
        this.addRenderableWidget<DowngradeButton?>(ammoDowngrade)
        this.addRenderableWidget<UpgradeButton?>(funcUpgrade)
        this.addRenderableWidget<DowngradeButton?>(funcDowngrade)
        this.addRenderableWidget<UpgradeButton?>(damageUpgrade)
        this.addRenderableWidget<DowngradeButton?>(damageDowngrade)
    }

    @OnlyIn(Dist.CLIENT)
    internal class ReforgeButton(pX: Int, pY: Int) : AbstractButton(pX, pY, 40, 16, Component.empty()) {
        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                (if (this.isHovered()) 81 else 51).toFloat(),
                184f,
                29,
                15,
                200,
                200
            )
        }

        override fun onPress() {
            sendPacketToServer(GunReforgeMessage)
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class UpgradeButton(pX: Int, pY: Int, var type: Perk.Type) :
        AbstractButton(pX, pY, 9, 9, Component.empty()) {
        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                187f,
                (if (this.isHovered()) 10 else 0).toFloat(),
                9,
                9,
                200,
                200
            )
        }

        override fun onPress() {
            if (this@ReforgingTableScreen.menu!!.getPerkItemBySlot(type) == ItemStack.EMPTY) {
                return
            }
            when (type) {
                Perk.Type.AMMO -> {
                    if (this@ReforgingTableScreen.menu!!.ammoPerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {
                        return
                    }
                }

                Perk.Type.FUNCTIONAL -> {
                    if (this@ReforgingTableScreen.menu!!.funcPerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {
                        return
                    }
                }

                Perk.Type.DAMAGE -> {
                    if (this@ReforgingTableScreen.menu!!.damagePerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {
                        return
                    }
                }
            }

            sendPacketToServer(SetPerkLevelMessage(type.ordinal, true))
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class DowngradeButton(pX: Int, pY: Int, var type: Perk.Type) :
        AbstractButton(pX, pY, 12, 12, Component.empty()) {
        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                177f,
                (if (this.isHovered()) 10 else 0).toFloat(),
                9,
                9,
                200,
                200
            )
        }

        override fun onPress() {
            if (this@ReforgingTableScreen.menu!!.getPerkItemBySlot(type) == ItemStack.EMPTY) {
                return
            }
            when (type) {
                Perk.Type.AMMO -> {
                    if (this@ReforgingTableScreen.menu!!.ammoPerkLevel.get() <= 1) {
                        return
                    }
                }

                Perk.Type.FUNCTIONAL -> {
                    if (this@ReforgingTableScreen.menu!!.funcPerkLevel.get() <= 1) {
                        return
                    }
                }

                Perk.Type.DAMAGE -> {
                    if (this@ReforgingTableScreen.menu!!.damagePerkLevel.get() <= 1) {
                        return
                    }
                }
            }

            sendPacketToServer(SetPerkLevelMessage(type.ordinal, false))
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/reforging_table.png")
    }
}