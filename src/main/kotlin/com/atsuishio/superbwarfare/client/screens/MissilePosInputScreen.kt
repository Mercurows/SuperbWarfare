package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
open class MissilePosInputScreen : Screen(Component.translatable("container.superbwarfare.missile_pos_input")) {
    lateinit var posX: EditBox
    lateinit var posY: EditBox
    lateinit var posZ: EditBox

    private var init = false

    protected var imageWidth: Int = 92
    protected var imageHeight: Int = 75

    override fun tick() {
        super.tick()
        if (!this.init) {
            val pos = ClientEventHandler.missileLockingPos
            if (pos != null) {
                this.posX.value = "${pos.x}"
                this.posY.value = "${pos.y}"
                this.posZ.value = "${pos.z}"
            }
            this.init = true
        }
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 128, 128)
        pGuiGraphics.drawString(this.font, this.title, i + 6, j + 3, 4210752, false)

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    protected fun renderPositions(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val poseStack = pGuiGraphics.pose()

        poseStack.pushPose()

        this.posX.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posY.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posZ.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        poseStack.popPose()
    }

    override fun init() {
        super.init()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        this.posX = EditBox(this.font, i + 19, j + 14, 60, 12, Component.empty())
        this.initEditBox(this.posX)

        this.posY = EditBox(this.font, i + 19, j + 27, 60, 12, Component.empty())
        this.initEditBox(this.posY)

        this.posZ = EditBox(this.font, i + 19, j + 40, 60, 12, Component.empty())
        this.initEditBox(this.posZ)

        val doneButton = this.DoneButton(i + 24, j + 54, 48, 15)
        this.addRenderableWidget(doneButton)
    }

    protected fun initEditBox(editBox: EditBox) {
        editBox.setCanLoseFocus(true)
        editBox.setTextColor(-1)
        editBox.setTextColorUneditable(-1)
        editBox.setBordered(false)
        editBox.setMaxLength(9)
        this.addWidget(editBox)
        editBox.setEditable(true)
        editBox.setFilter { it.matches("-?\\d*".toRegex()) }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class DoneButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            if (!this@MissilePosInputScreen.init) return
            if (this@MissilePosInputScreen.minecraft != null) {
                this@MissilePosInputScreen.minecraft!!.setScreen(null)
            }

            val x = getEditBoxValue(this@MissilePosInputScreen.posX.value) ?: return
            val y = getEditBoxValue(this@MissilePosInputScreen.posY.value) ?: return
            val z = getEditBoxValue(this@MissilePosInputScreen.posZ.value) ?: return
            ClientEventHandler.missileLockingPos = BlockPos(x, y, z)
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                0f,
                if (this.isHovered) 92f else 76f,
                48,
                15,
                128,
                128
            )
        }

        fun getEditBoxValue(value: String): Int? {
            return try {
                value.toInt()
            } catch (_: NumberFormatException) {
                null
            }
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/missile_pos_input.png")
    }
}