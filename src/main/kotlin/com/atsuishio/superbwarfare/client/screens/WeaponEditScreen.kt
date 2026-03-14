package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.event.ClientEventHandler.editModelShake
import com.atsuishio.superbwarfare.event.ClientEventHandler.onCloseEditScreen
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.EditMessage
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
class WeaponEditScreen(private val stack: ItemStack) : Screen(Component.empty()) {
    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderEdit(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    fun renderEdit(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        if (stack.item !is GunItem) return
        val player = localPlayer ?: return
        val itemStack = player.mainHandItem
        val item = itemStack.item
        if (item !is GunItem) return
        if (item !== stack.item) return

        val pose = pGuiGraphics.pose()

        pose.pushPose()

        pGuiGraphics.fill(this.width - 165, 4, this.width - 4, 110, -0x80000000)
        pGuiGraphics.drawString(this.font, this.stack.getHoverName(), this.width - 161, 6, 0xFFFFFF, false)
        pGuiGraphics.fill(
            this.width - 163,
            16,
            min(this.width + this.font.width(this.stack.getHoverName()) - 159, this.width - 6),
            17,
            -0x1
        )

        val posX1 = this.width - 163
        val posX2 = this.width - 85

        val posY1 = 20
        val posY2 = 50
        val posY3 = 80

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        val data = from(stack)

        RenderHelper.preciseBlit(pGuiGraphics, BARREL, posX1.toFloat(), posY1.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (!item.hasCustomBarrel(data)) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX1.toFloat(),
                posY1.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        }

        RenderHelper.preciseBlit(pGuiGraphics, SCOPE, posX2.toFloat(), posY1.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (!item.hasCustomScope(data)) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX2.toFloat(),
                posY1.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        }

        RenderHelper.preciseBlit(pGuiGraphics, GRIP, posX1.toFloat(), posY2.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (!item.hasCustomGrip(data)) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX1.toFloat(),
                posY2.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        }

        RenderHelper.preciseBlit(pGuiGraphics, STOCK, posX2.toFloat(), posY2.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (!item.hasCustomStock(data)) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX2.toFloat(),
                posY2.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        }

        RenderHelper.preciseBlit(pGuiGraphics, MAGAZINE, posX1.toFloat(), posY3.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (!item.hasCustomMagazine(data)) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX1.toFloat(),
                posY3.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        }

        val currentData = from(itemStack)
        RenderHelper.preciseBlit(pGuiGraphics, AMMO_TYPE, posX2.toFloat(), posY3.toFloat(), 0f, 0f, 24f, 24f, 24f, 24f)
        if (currentData.get(GunProp.AMMO_CONSUMER).size <= 1) {
            RenderHelper.preciseBlit(
                pGuiGraphics,
                INVALID,
                posX2.toFloat(),
                posY3.toFloat(),
                0f,
                0f,
                24f,
                24f,
                24f,
                24f
            )
        } else {
            val size = currentData.get(GunProp.AMMO_CONSUMER).size
            val offset = 35f
            val count = size / 2

            val tempPos = (if (size % 2 == 0) this.width - count * 6 + 1 else this.width - count * 6 - 2).toFloat()
            for (i in 0..<size) {
                RenderHelper.preciseBlit(
                    pGuiGraphics,
                    if (i == currentData.selectedAmmoType.get()) CHOSEN else NOT_CHOSEN,
                    tempPos - offset + 6 * i, posY3.toFloat(), 0f, 0f,
                    4f, 4f, 4f, 4f
                )
            }
        }

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        pose.popPose()
    }

    override fun init() {
        super.init()

        val posX1 = this.width - 133
        val posX2 = this.width - 55

        val posY1 = 26
        val posY2 = 56
        val posY3 = 86

        this.addRenderableWidget(EditButton(posX1, posY1, 16, 16, 0, true))
        this.addRenderableWidget(EditButton(posX1 + 24, posY1, 16, 16, 0, false))

        this.addRenderableWidget(EditButton(posX2, posY1, 16, 16, 1, true))
        this.addRenderableWidget(EditButton(posX2 + 24, posY1, 16, 16, 1, false))

        this.addRenderableWidget(EditButton(posX1, posY2, 16, 16, 2, true))
        this.addRenderableWidget(EditButton(posX1 + 24, posY2, 16, 16, 2, false))

        this.addRenderableWidget(EditButton(posX2, posY2, 16, 16, 3, true))
        this.addRenderableWidget(EditButton(posX2 + 24, posY2, 16, 16, 3, false))

        this.addRenderableWidget(EditButton(posX1, posY3, 16, 16, 4, true))
        this.addRenderableWidget(EditButton(posX1 + 24, posY3, 16, 16, 4, false))

        this.addRenderableWidget(EditButton(posX2, posY3, 16, 16, 5, true))
        this.addRenderableWidget(EditButton(posX2 + 24, posY3, 16, 16, 5, false))
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        if (pMouseX < this.width - 165 || pMouseY < 4 || pMouseX > this.width - 4 || pMouseY > 110) {
            this.onClose()
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun onClose() {
        super.onClose()
        onCloseEditScreen()
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        if (pKeyCode == ModKeyMappings.EDIT_MODE.key.value) {
            this.onClose()
            return true
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers)
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class EditButton(
        pX: Int,
        pY: Int,
        pWidth: Int,
        pHeight: Int, // 0 = barrel, 1 = scope, 2 = grip, 3 = stock, 4 = magazine, 5 = ammoType
        var type: Int,
        var left: Boolean
    ) : AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.pose().pushPose()

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            )

            if (this.isHovered && this.isActive()) {
                pGuiGraphics.blit(
                    if (this.left) BUTTON_LEFT_HOVERED else BUTTON_RIGHT_HOVERED, this.x, this.y,
                    0f, 0f, 16, 16, 16, 16
                )
            } else {
                pGuiGraphics.blit(
                    if (this.left) BUTTON_LEFT else BUTTON_RIGHT, this.x, this.y,
                    0f, 0f, 16, 16, 16, 16
                )
            }

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            pGuiGraphics.pose().popPose()
        }

        override fun onPress() {
            if (!this.isActive()) return
            sendPacketToServer(EditMessage(this.type, !this.left, false))
            editModelShake()
        }

        override fun isActive(): Boolean {
            val stack = this@WeaponEditScreen.stack
            val item = stack.item
            if (item !is GunItem) return false
            val data = from(stack)

            return when (this.type) {
                0 -> item.hasCustomBarrel(data)
                1 -> item.hasCustomScope(data)
                2 -> item.hasCustomGrip(data)
                3 -> item.hasCustomStock(data)
                4 -> item.hasCustomMagazine(data)
                5 -> data.get(GunProp.AMMO_CONSUMER).size > 1
                else -> false
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    companion object {
        // 六个改装位置，大小128*128
        private val BARREL = loc("textures/gui/attachment/barrel.png")
        private val SCOPE = loc("textures/gui/attachment/scope.png")
        private val GRIP = loc("textures/gui/attachment/grip.png")
        private val STOCK = loc("textures/gui/attachment/stock.png")
        private val MAGAZINE = loc("textures/gui/attachment/magazine.png")
        private val AMMO_TYPE = loc("textures/gui/attachment/ammo_type.png")

        // 配件不可用标识，大小128*128
        private val INVALID = loc("textures/gui/attachment/invalid.png")

        // 按钮，大小64*64
        private val BUTTON_LEFT = loc("textures/gui/attachment/button_left.png")
        private val BUTTON_RIGHT = loc("textures/gui/attachment/button_right.png")
        private val BUTTON_LEFT_HOVERED = loc("textures/gui/attachment/button_left_hovered.png")
        private val BUTTON_RIGHT_HOVERED = loc("textures/gui/attachment/button_right_hovered.png")

        // 标记，大小16*16
        private val CHOSEN = loc("textures/gui/attachment/chosen.png")
        private val NOT_CHOSEN = loc("textures/gui/attachment/not_chosen.png")
    }
}
