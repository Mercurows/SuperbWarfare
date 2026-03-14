package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.message.send.FiringParametersEditMessage
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class FiringParametersScreen(private val stack: ItemStack, private val hand: InteractionHand) :
    Screen(GameNarrator.NO_TITLE) {
    lateinit var posX: EditBox
    lateinit var posY: EditBox
    lateinit var posZ: EditBox
    lateinit var radius: EditBox

    var isDepressed: Boolean = false

    private var init = false

    protected var imageWidth: Int = 94
    protected var imageHeight: Int = 126

    init {
        if (!stack.isEmpty) {
            this.isDepressed = stack.firingParameters.isDepressed
        }
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun tick() {
        super.tick()
        if (!this.init) {
            if (!this.stack.isEmpty) {
                val parameters = stack.firingParameters
                val pos = parameters.pos
                this.posX.value = "${pos.x}"
                this.posY.value = "${pos.y}"
                this.posZ.value = "${pos.z}"
                this.radius.value = "${max(0, parameters.radius)}"
            }
            this.init = true
        }
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        this.renderBg(pGuiGraphics, pMouseX, pMouseY)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    protected fun renderPositions(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val poseStack = pGuiGraphics.pose()

        poseStack.pushPose()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        poseStack.rotateAround(Axis.ZP.rotationDegrees(5f), (i + 41).toFloat(), (j + 22).toFloat(), 0f)

        this.posX.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posY.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posZ.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.radius.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        poseStack.popPose()
    }

    protected fun renderBg(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 140, 140)

        if (pMouseX >= i + 12 && pMouseX <= i + 47 && pMouseY >= j + 89 && pMouseY <= j + 109) {
            pGuiGraphics.renderTooltip(
                this.font,
                if (this.isDepressed) Component.translatable("tips.superbwarfare.mortar.target_pos.depressed_trajectory")
                    .withStyle(ChatFormatting.WHITE) else Component.translatable("tips.superbwarfare.mortar.target_pos.lofted_trajectory")
                    .withStyle(ChatFormatting.WHITE),
                pMouseX, pMouseY
            )
        }
    }

    override fun init() {
        super.init()
        this.subInit()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val modeButton = this.ModeButton(i + 12, j + 89, 35, 20)
        this.addRenderableWidget(modeButton)

        val doneButton = this.DoneButton(i + 50, j + 94, 23, 14)
        this.addRenderableWidget(doneButton)
    }

    protected fun subInit() {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        this.posX = EditBox(this.font, i + 44, j + 20, 60, 12, Component.empty())
        this.initEditBox(this.posX)

        this.posY = EditBox(this.font, i + 43, j + 37, 60, 12, Component.empty())
        this.initEditBox(this.posY)

        this.posZ = EditBox(this.font, i + 42, j + 54, 60, 12, Component.empty())
        this.initEditBox(this.posZ)

        this.radius = EditBox(this.font, i + 41, j + 71, 20, 12, Component.empty())
        this.initEditBox(this.radius)
        this.radius.setMaxLength(2)
        this.radius.setFilter { it.matches("\\d*".toRegex()) }
    }

    protected fun initEditBox(editBox: EditBox) {
        editBox.setCanLoseFocus(true)
        editBox.setTextColor(0xb29f7c)
        editBox.setTextColorUneditable(0x5b4c3c)
        editBox.setBordered(false)
        editBox.setMaxLength(9)
        this.addWidget(editBox)
        editBox.setEditable(true)
        editBox.setFilter { it.matches("-?\\d*".toRegex()) }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class ModeButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            this@FiringParametersScreen.isDepressed = !this@FiringParametersScreen.isDepressed
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            val isDepressed = this@FiringParametersScreen.isDepressed
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                if (isDepressed) this.y + 10 else this.y,
                96f,
                (if (isDepressed) 37 else 16).toFloat(),
                35,
                if (isDepressed) 10 else 20,
                140,
                140
            )
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class DoneButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            if (!this@FiringParametersScreen.init) return
            if (this@FiringParametersScreen.minecraft != null) {
                this@FiringParametersScreen.minecraft!!.setScreen(null)
            }
            sendPacketToServer(
                FiringParametersEditMessage(
                    getEditBoxValue(this@FiringParametersScreen.posX.value),
                    getEditBoxValue(this@FiringParametersScreen.posY.value),
                    getEditBoxValue(this@FiringParametersScreen.posZ.value),
                    max(0, getEditBoxValue(this@FiringParametersScreen.radius.value)),
                    this@FiringParametersScreen.isDepressed,
                    this@FiringParametersScreen.hand == InteractionHand.MAIN_HAND
                )
            )
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            if (this.isHovered) {
                pGuiGraphics.blit(TEXTURE, this.x, this.y, 95f, 1f, 23, 14, 140, 140)
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        fun getEditBoxValue(value: String): Int {
            if (value == "-") return 0
            return try {
                value.toInt()
            } catch (_: NumberFormatException) {
                0
            }
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/firing_parameters.png")
    }
}
